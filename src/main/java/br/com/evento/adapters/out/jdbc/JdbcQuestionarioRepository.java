package br.com.evento.adapters.out.jdbc;

import br.com.evento.domain.model.entity.Pergunta;
import br.com.evento.domain.model.entity.Questionario;
import br.com.evento.domain.model.entity.TipoPergunta;
import br.com.evento.domain.port.out.QuestionarioRepository;
import br.com.evento.infra.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Adapter JDBC puro da porta QuestionarioRepository.
 */
public class JdbcQuestionarioRepository implements QuestionarioRepository {

    static final String SEPARADOR = "|||";

    private final ConnectionFactory factory;

    public JdbcQuestionarioRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Questionario salvar(Questionario questionario) {
        // Coluna politica_identificacao (V9/RN-15) pode não existir em bancos antigos.
        boolean temPolitica = false;
        try (Connection probe = factory.open()) {
            try (ResultSet rs = probe.getMetaData().getColumns(null, null, "questionarios",
                    "politica_identificacao")) {
                temPolitica = rs.next();
            }
        } catch (Exception ignored) {
        }
        String upsert = temPolitica
                ? "INSERT INTO questionarios (id, evento_id, titulo, congelado, politica_identificacao)"
                        + " VALUES (?,?,?,?,?)"
                        + " ON CONFLICT (id) DO UPDATE SET evento_id=EXCLUDED.evento_id,"
                        + " titulo=EXCLUDED.titulo, congelado=EXCLUDED.congelado,"
                        + " politica_identificacao=EXCLUDED.politica_identificacao"
                : "INSERT INTO questionarios (id, evento_id, titulo, congelado) VALUES (?,?,?,?)"
                        + " ON CONFLICT (id) DO UPDATE SET evento_id=EXCLUDED.evento_id,"
                        + " titulo=EXCLUDED.titulo, congelado=EXCLUDED.congelado";
        try (Connection c = factory.open()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(upsert)) {
                ps.setString(1, questionario.getId());
                ps.setString(2, questionario.getEventoId());
                ps.setString(3, questionario.getTitulo());
                ps.setBoolean(4, questionario.isCongelado());
                if (temPolitica) ps.setString(5, questionario.getPoliticaIdentificacao());
                ps.executeUpdate();
            }
            // Upsert por ID + DELETE só de órfãs: um DELETE geral acionaria o
            // ON DELETE CASCADE de respostas(pergunta_id) e apagaria respostas (RF-28).
            String ins = "INSERT INTO perguntas (id, questionario_id, enunciado, tipo, obrigatoria, opcoes, escala_max)"
                    + " VALUES (?,?,?,?,?,?,?)"
                    + " ON CONFLICT (id) DO UPDATE SET enunciado=EXCLUDED.enunciado,"
                    + " tipo=EXCLUDED.tipo, obrigatoria=EXCLUDED.obrigatoria,"
                    + " opcoes=EXCLUDED.opcoes, escala_max=EXCLUDED.escala_max";
            for (Pergunta p : questionario.getPerguntas()) {
                try (PreparedStatement ps = c.prepareStatement(ins)) {
                    ps.setString(1, p.getId());
                    ps.setString(2, questionario.getId());
                    ps.setString(3, p.getEnunciado());
                    ps.setString(4, p.getTipo().name());
                    ps.setBoolean(5, p.isObrigatoria());
                    ps.setString(6, p.getOpcoes().isEmpty() ? null : String.join(SEPARADOR, p.getOpcoes()));
                    if (p.getEscalaMax() == null) ps.setNull(7, java.sql.Types.INTEGER);
                    else ps.setInt(7, p.getEscalaMax());
                    ps.executeUpdate();
                }
            }
            if (questionario.getPerguntas().isEmpty()) {
                try (PreparedStatement del = c.prepareStatement(
                        "DELETE FROM perguntas WHERE questionario_id = ?")) {
                    del.setString(1, questionario.getId());
                    del.executeUpdate();
                }
            } else {
                String placeholders = questionario.getPerguntas().stream()
                        .map(p -> "?").collect(java.util.stream.Collectors.joining(","));
                try (PreparedStatement del = c.prepareStatement(
                        "DELETE FROM perguntas WHERE questionario_id = ? AND id NOT IN (" + placeholders + ")")) {
                    del.setString(1, questionario.getId());
                    int i = 2;
                    for (Pergunta p : questionario.getPerguntas()) del.setString(i++, p.getId());
                    del.executeUpdate();
                }
            }
            c.commit();
            return questionario;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao salvar questionário", e);
        }
    }

    private String colunasQuestionario(Connection c) throws Exception {
        // RN-15: coluna pode não existir em bancos pré-V9
        if (temColuna(c, "questionarios", "politica_identificacao")) {
            return "id, evento_id, titulo, congelado, politica_identificacao";
        }
        return "id, evento_id, titulo, congelado";
    }

    @Override
    public Optional<Questionario> buscarPorId(String id) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT " + colunasQuestionario(c) + " FROM questionarios WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(reconstituir(c, rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar questionário", e);
        }
    }

    @Override
    public List<Questionario> buscarPorEvento(String eventoId) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT " + colunasQuestionario(c) + " FROM questionarios WHERE evento_id = ?")) {
            ps.setString(1, eventoId);
            List<Questionario> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(reconstituir(c, rs));
            }
            return lista;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar questionários", e);
        }
    }

    private static boolean temColuna(Connection c, String tabela, String coluna) throws Exception {
        try (ResultSet rs = c.getMetaData().getColumns(null, null, tabela, coluna)) {
            return rs.next();
        }
    }

    private Questionario reconstituir(Connection c, ResultSet rs) throws Exception {
        Questionario q = new Questionario(rs.getString("id"), rs.getString("evento_id"), rs.getString("titulo"));
        // RN-15: coluna pode não existir em bancos pré-V9
        if (temColuna(c, "questionarios", "politica_identificacao")) {
            try {
                q.restaurarPolitica(rs.getString("politica_identificacao"));
            } catch (Exception ignored) {
            }
        }
        String sql = "SELECT id, enunciado, tipo, obrigatoria, opcoes, escala_max FROM perguntas"
                + " WHERE questionario_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, q.getId());
            try (ResultSet rp = ps.executeQuery()) {
                while (rp.next()) {
                    String opcoesRaw = rp.getString("opcoes");
                    List<String> opcoes = (opcoesRaw == null || opcoesRaw.isEmpty())
                            ? List.of()
                            : Arrays.asList(opcoesRaw.split(java.util.regex.Pattern.quote(SEPARADOR), -1));
                    Object escala = rp.getObject("escala_max");
                    q.adicionarPergunta(new Pergunta(
                            rp.getString("id"),
                            rp.getString("enunciado"),
                            TipoPergunta.valueOf(rp.getString("tipo")),
                            rp.getBoolean("obrigatoria"),
                            opcoes,
                            escala == null ? null : ((Number) escala).intValue()));
                }
            }
        }
        if (rs.getBoolean("congelado")) q.congelar();
        return q;
    }
}
