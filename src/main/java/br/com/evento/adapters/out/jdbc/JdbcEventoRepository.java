package br.com.evento.adapters.out.jdbc;

import br.com.evento.domain.model.entity.Administrador;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.StatusEvento;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.infra.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adapter JDBC puro da porta EventoRepository (sem JPA/Hibernate).
 * Atividades são carregadas junto (uma query por evento).
 */
public class JdbcEventoRepository implements EventoRepository {

    private final ConnectionFactory factory;

    public JdbcEventoRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Evento salvar(Evento evento) {
        // Colunas V9 podem não existir em bancos antigos: monta upsert conforme o esquema.
        boolean temV9 = false;
        try (Connection probe = factory.open()) {
            temV9 = temColuna(probe, "eventos", "fuso");
        } catch (Exception ignored) {
        }
        String upsert = temV9
                ? "INSERT INTO eventos (id, titulo, descricao, status, organizador_id, max_vagas,"
                        + " periodo_inicio, periodo_fim, local, fuso,"
                        + " exige_escolha_atividades, vagas_por_atividade)"
                        + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"
                        + " ON CONFLICT (id) DO UPDATE SET titulo=EXCLUDED.titulo,"
                        + " descricao=EXCLUDED.descricao, status=EXCLUDED.status,"
                        + " organizador_id=EXCLUDED.organizador_id, max_vagas=EXCLUDED.max_vagas,"
                        + " periodo_inicio=EXCLUDED.periodo_inicio, periodo_fim=EXCLUDED.periodo_fim,"
                        + " local=EXCLUDED.local, fuso=EXCLUDED.fuso,"
                        + " exige_escolha_atividades=EXCLUDED.exige_escolha_atividades,"
                        + " vagas_por_atividade=EXCLUDED.vagas_por_atividade"
                : "INSERT INTO eventos (id, titulo, descricao, status, organizador_id, max_vagas)"
                        + " VALUES (?,?,?,?,?,?)"
                        + " ON CONFLICT (id) DO UPDATE SET titulo=EXCLUDED.titulo,"
                        + " descricao=EXCLUDED.descricao, status=EXCLUDED.status,"
                        + " organizador_id=EXCLUDED.organizador_id, max_vagas=EXCLUDED.max_vagas";
        try (Connection c = factory.open()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(upsert)) {
                ps.setString(1, evento.getId());
                ps.setString(2, evento.getTitulo());
                ps.setString(3, evento.getDescricao());
                ps.setString(4, evento.getStatus().name());
                ps.setString(5, evento.getOrganizador().getId());
                if (evento.getMaxVagas() == null) ps.setNull(6, java.sql.Types.INTEGER);
                else ps.setInt(6, evento.getMaxVagas());
                if (temV9) {
                    if (evento.getPeriodo() == null) {
                        ps.setNull(7, java.sql.Types.TIMESTAMP);
                        ps.setNull(8, java.sql.Types.TIMESTAMP);
                    } else {
                        ps.setTimestamp(7, Timestamp.valueOf(evento.getPeriodo().getInicio()));
                        ps.setTimestamp(8, Timestamp.valueOf(evento.getPeriodo().getFim()));
                    }
                    ps.setString(9, evento.getLocal());
                    ps.setString(10, evento.getFuso());
                    ps.setBoolean(11, evento.isExigeEscolhaAtividades());
                    ps.setBoolean(12, evento.isVagasPorAtividade());
                }
                ps.executeUpdate();
            }
            // Upsert por ID + DELETE só de órfãs: um DELETE geral acionaria o
            // ON DELETE CASCADE de checkins(atividade_id) e apagaria check-ins (RN-11),
            // ou violaria a FK de inscricao_atividades(atividade_id).
            boolean temAtExtras = temColuna(c, "atividades", "tipo");
            boolean temPolitica = temAtExtras && temColuna(c, "atividades", "politica_frequencia");
            // V11 pode não existir em bancos antigos: monta upsert conforme o esquema.
            boolean temMaxVagas = temColuna(c, "atividades", "max_vagas");
            String ins = "INSERT INTO atividades (id, titulo, inicio, fim, local, evento_id, tipo, trilha"
                    + (temPolitica ? ", politica_frequencia" : "")
                    + (temMaxVagas ? ", max_vagas" : "") + ")"
                    + " VALUES (?,?,?,?,?,?,?,?" + (temPolitica ? ",?" : "") + (temMaxVagas ? ",?" : "") + ")"
                    + " ON CONFLICT (id) DO UPDATE SET titulo=EXCLUDED.titulo,"
                    + " inicio=EXCLUDED.inicio, fim=EXCLUDED.fim, local=EXCLUDED.local,"
                    + " evento_id=EXCLUDED.evento_id, tipo=EXCLUDED.tipo, trilha=EXCLUDED.trilha"
                    + (temPolitica ? ", politica_frequencia=EXCLUDED.politica_frequencia" : "")
                    + (temMaxVagas ? ", max_vagas=EXCLUDED.max_vagas" : "");
            for (Atividade a : evento.getAtividades()) {
                try (PreparedStatement ps = c.prepareStatement(ins)) {
                    ps.setString(1, a.getId());
                    ps.setString(2, a.getTitulo());
                    ps.setTimestamp(3, Timestamp.valueOf(a.getPeriodo().getInicio()));
                    ps.setTimestamp(4, Timestamp.valueOf(a.getPeriodo().getFim()));
                    ps.setString(5, a.getLocal());
                    ps.setString(6, evento.getId());
                    ps.setString(7, temAtExtras ? a.getTipo() : null);
                    ps.setString(8, temAtExtras ? a.getTrilha() : null);
                    int idx = 9;
                    if (temPolitica) ps.setString(idx++, a.getNomePoliticaFrequencia());
                    if (temMaxVagas) {
                        if (a.getMaxVagas() == null) ps.setNull(idx++, java.sql.Types.INTEGER);
                        else ps.setInt(idx++, a.getMaxVagas());
                    }
                    ps.executeUpdate();
                }
            }
            if (evento.getAtividades().isEmpty()) {
                try (PreparedStatement del = c.prepareStatement(
                        "DELETE FROM atividades WHERE evento_id = ?")) {
                    del.setString(1, evento.getId());
                    del.executeUpdate();
                }
            } else {
                String placeholders = evento.getAtividades().stream()
                        .map(a -> "?").collect(java.util.stream.Collectors.joining(","));
                try (PreparedStatement del = c.prepareStatement(
                        "DELETE FROM atividades WHERE evento_id = ? AND id NOT IN (" + placeholders + ")")) {
                    del.setString(1, evento.getId());
                    int i = 2;
                    for (Atividade a : evento.getAtividades()) del.setString(i++, a.getId());
                    del.executeUpdate();
                }
            }
            c.commit();
            return evento;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao salvar evento", e);
        }
    }

    @Override
    public Optional<Evento> buscarPorId(String id) {
        return Optional.ofNullable(buscarUm(sqlComV9SeExistir("e.id = ?"), id));
    }

    @Override
    public List<Evento> buscarPorStatus(StatusEvento status) {
        return buscarVarios(sqlComV9SeExistir("e.status = ?"), status.name());
    }

    @Override
    public List<Evento> buscarPorOrganizador(String organizadorId) {
        return buscarVarios(sqlComV9SeExistir("e.organizador_id = ?"), organizadorId);
    }

    @Override
    public void remover(String id) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement("DELETE FROM eventos WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao remover evento", e);
        }
    }

    private static final String COLS_BASE = "e.id, e.titulo, e.descricao, e.status,"
            + " e.organizador_id, e.max_vagas, u.nome, u.email, u.senha_hash, u.tipo_usuario";
    private static final String COLS_V9 = ", e.periodo_inicio, e.periodo_fim, e.local,"
            + " e.fuso, e.exige_escolha_atividades, e.vagas_por_atividade";

    private static String sqlEvento(String where) {
        return "SELECT " + COLS_BASE + " FROM eventos e"
                + " JOIN usuarios u ON u.id = e.organizador_id WHERE " + where;
    }

    private static String sqlEventoV9(String where) {
        return "SELECT " + COLS_BASE + COLS_V9 + " FROM eventos e"
                + " JOIN usuarios u ON u.id = e.organizador_id WHERE " + where;
    }

    private static Evento mapearEvento(ResultSet rs) throws Exception {
        return mapearEvento(rs, false);
    }

    private static Evento mapearEvento(ResultSet rs, boolean temV9) throws Exception {
        String tipoDono = null;
        try {
            tipoDono = rs.getString("tipo_usuario");
        } catch (Exception ignored) {
            // Bancos sem a coluna (pré-V13): o dono segue como organizador.
        }
        Usuario dono;
        if ("ADMINISTRADOR".equalsIgnoreCase(tipoDono)) {
            dono = new Administrador(
                    rs.getString("organizador_id"),
                    rs.getString("nome"),
                    new Email(rs.getString("email")),
                    new SenhaHash(rs.getString("senha_hash")));
        } else {
            dono = new Organizador(
                    rs.getString("organizador_id"),
                    rs.getString("nome"),
                    new Email(rs.getString("email")),
                    new SenhaHash(rs.getString("senha_hash")));
        }
        Evento evento = new Evento(rs.getString("id"), rs.getString("titulo"),
                rs.getString("descricao"), dono);
        evento.restaurarEstado(StatusEvento.valueOf(rs.getString("status")));
        Object maxVagas = rs.getObject("max_vagas");
        if (maxVagas != null) evento.configurarLimiteDeVagas(((Number) maxVagas).intValue());
        if (temV9) {
            java.sql.Timestamp ini = rs.getTimestamp("periodo_inicio");
            java.sql.Timestamp fim = rs.getTimestamp("periodo_fim");
            br.com.evento.domain.model.vo.Periodo periodo = (ini != null && fim != null)
                    ? new br.com.evento.domain.model.vo.Periodo(
                            ini.toLocalDateTime(), fim.toLocalDateTime())
                    : null;
            evento.restaurarDetalhes(periodo, rs.getString("local"), rs.getString("fuso"),
                    rs.getBoolean("exige_escolha_atividades"), rs.getBoolean("vagas_por_atividade"));
        }
        return evento;
    }

    private Evento buscarUm(String sql, String param) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Evento evento = mapearEvento(rs, sql.contains("periodo_inicio"));
                carregarAtividades(c, evento);
                return evento;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar evento", e);
        }
    }

    private List<Evento> buscarVarios(String sql, String param) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            List<Evento> eventos = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                boolean temV9 = sql.contains("periodo_inicio");
                while (rs.next()) {
                    Evento evento = mapearEvento(rs, temV9);
                    carregarAtividades(c, evento);
                    eventos.add(evento);
                }
            }
            return eventos;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar eventos", e);
        }
    }

    private String sqlComV9SeExistir(String where) {
        try (Connection c = factory.open()) {
            if (temColuna(c, "eventos", "fuso")) return sqlEventoV9(where);
        } catch (Exception ignored) {
        }
        return sqlEvento(where);
    }

    private static void carregarAtividades(Connection c, Evento evento) throws Exception {
        // Colunas tipo/trilha (V6), politica_frequencia (V9) e max_vagas (V11)
        // podem não existir em bancos antigos.
        boolean temExtras = temColuna(c, "atividades", "tipo");
        boolean temPolitica = temExtras && temColuna(c, "atividades", "politica_frequencia");
        boolean temMaxVagas = temColuna(c, "atividades", "max_vagas");
        String base = " FROM atividades WHERE evento_id = ? ORDER BY inicio";
        String sql = "SELECT id, titulo, inicio, fim, local"
                + (temExtras ? ", tipo, trilha" : "")
                + (temPolitica ? ", politica_frequencia" : "")
                + (temMaxVagas ? ", max_vagas" : "") + base;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, evento.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Atividade a = new Atividade(
                            rs.getString("id"),
                            rs.getString("titulo"),
                            new Periodo(rs.getTimestamp("inicio").toLocalDateTime(),
                                    rs.getTimestamp("fim").toLocalDateTime()),
                            rs.getString("local"),
                            temExtras ? rs.getString("tipo") : null,
                            temExtras ? rs.getString("trilha") : null);
                    if (temPolitica) {
                        String pol = rs.getString("politica_frequencia");
                        if (pol != null && !pol.isBlank()) a.definirPoliticaFrequencia(pol);
                    }
                    if (temMaxVagas) {
                        Object mv = rs.getObject("max_vagas");
                        if (mv != null) a.configurarLimiteDeVagas(((Number) mv).intValue());
                    }
                    evento.adicionarAtividade(a);
                }
            }
        }
    }

    private static boolean temColuna(Connection c, String tabela, String coluna) throws Exception {
        try (ResultSet rs = c.getMetaData().getColumns(null, null, tabela, coluna)) {
            return rs.next();
        }
    }
}
