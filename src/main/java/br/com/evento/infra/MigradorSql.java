package br.com.evento.infra;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Migrador de esquema minimalista (substitui o Flyway).
 * Aplica V1..Vn em ordem, registrando em schema_version.
 * Migrations ficam em src/main/resources/db/migration/.
 */
public class MigradorSql {

    private final ConnectionFactory factory;

    public MigradorSql(ConnectionFactory factory) {
        this.factory = factory;
    }

    public void migrar() {
        try (Connection c = factory.open(); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS schema_version (versao INTEGER PRIMARY KEY)");
            importarFlyway(st);
            int atual = versaoAtual(st);
            for (int v = atual + 1; ; v++) {
                String sql = lerMigration(v);
                if (sql == null) break;
                for (String comando : quebrar(sql)) {
                    try (Statement s2 = c.createStatement()) {
                        s2.execute(comando);
                    }
                }
                try (Statement s2 = c.createStatement()) {
                    s2.execute("INSERT INTO schema_version (versao) VALUES (" + v + ")");
                }
                System.out.println("[Migrador] aplicada V" + v);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha na migração do banco", e);
        }
    }

    /**
     * Bancos criados na era Spring/Flyway já têm V1..V5 aplicadas.
     * Importa a versão do flyway_schema_history para não reaplicar.
     */
    private void importarFlyway(Statement st) throws Exception {
        int atual = versaoAtual(st);
        if (atual > 0) return;
        try (ResultSet rs = st.executeQuery(
                "SELECT max(CAST(version AS INTEGER)) FROM flyway_schema_history WHERE success = true")) {
            if (rs.next()) {
                int flyway = rs.getInt(1);
                if (!rs.wasNull() && flyway > 0) {
                    try (Statement s2 = st.getConnection().createStatement()) {
                        for (int v = 1; v <= flyway; v++) {
                            s2.execute("INSERT INTO schema_version (versao) VALUES (" + v + ")"
                                    + " ON CONFLICT (versao) DO NOTHING");
                        }
                    }
                    System.out.println("[Migrador] baseline Flyway importado: V" + flyway);
                }
            }
        } catch (Exception ignored) {
            // Sem tabela flyway (base nova): segue do zero
        }
    }

    private int versaoAtual(Statement st) throws Exception {
        try (ResultSet rs = st.executeQuery("SELECT max(versao) FROM schema_version")) {
            if (rs.next()) {
                int v = rs.getInt(1);
                return rs.wasNull() ? 0 : v;
            }
            return 0;
        }
    }

    private String lerMigration(int versao) {
        String[] candidatos = {
            "db/migration/V" + versao + "__create_initial_schema.sql",
            "db/migration/V" + versao + "__create_inscricoes_schema.sql",
            "db/migration/V" + versao + "__insert_initial_data.sql",
            "db/migration/V" + versao + "__create_avaliacoes_schema.sql",
            "db/migration/V" + versao + "__create_checkins_table.sql",
        };
        for (String nome : candidatos) {
            InputStream in = getClass().getClassLoader().getResourceAsStream(nome);
            if (in != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    return r.lines().collect(Collectors.joining("\n"));
                } catch (Exception e) {
                    throw new IllegalStateException("Falha ao ler " + nome, e);
                }
            }
        }
        return null;
    }

    /** Quebra por ';' fora de strings simples; suficiente para nosso DDL. */
    static List<String> quebrar(String sql) {
        List<String> comandos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean emString = false;
        String semComentarios = sql.replaceAll("(?m)^\\s*--.*$", "");
        for (int i = 0; i < semComentarios.length(); i++) {
            char ch = semComentarios.charAt(i);
            if (ch == '\'') emString = !emString;
            if (ch == ';' && !emString) {
                String cmd = atual.toString().trim();
                if (!cmd.isEmpty()) comandos.add(cmd);
                atual.setLength(0);
            } else {
                atual.append(ch);
            }
        }
        String resto = atual.toString().trim();
        if (!resto.isEmpty()) comandos.add(resto);
        return comandos;
    }
}
