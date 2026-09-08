package br.com.evento.adapters.out.jdbc;

import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.InscricaoResumo;
import br.com.evento.infra.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter JDBC puro da porta InscricaoRepository (sem JPA/Hibernate).
 * Check-ins vivem em tabela própria (V5) e são reanexados ao agregado (RN-11).
 */
public class JdbcInscricaoRepository implements InscricaoRepository {

    private final ConnectionFactory factory;

    public JdbcInscricaoRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Inscricao salvar(Inscricao inscricao) {
        String upsert = "INSERT INTO inscricoes (id, participante_id, evento_id, data_inscricao)"
                + " VALUES (?,?,?,?)"
                + " ON CONFLICT (id) DO UPDATE SET participante_id=EXCLUDED.participante_id,"
                + " evento_id=EXCLUDED.evento_id, data_inscricao=EXCLUDED.data_inscricao";
        try (Connection c = factory.open()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(upsert)) {
                ps.setString(1, inscricao.getId());
                ps.setString(2, inscricao.getParticipanteId());
                ps.setString(3, inscricao.getEventoId());
                ps.setTimestamp(4, Timestamp.valueOf(inscricao.getDataInscricao()));
                ps.executeUpdate();
            }
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM inscricao_atividades WHERE inscricao_id = ?")) {
                del.setString(1, inscricao.getId());
                del.executeUpdate();
            }
            String ins = "INSERT INTO inscricao_atividades (inscricao_id, atividade_id)"
                    + " VALUES (?,?) ON CONFLICT DO NOTHING";
            for (Atividade a : inscricao.getAtividadesSelecionadas()) {
                try (PreparedStatement ps = c.prepareStatement(ins)) {
                    ps.setString(1, inscricao.getId());
                    ps.setString(2, a.getId());
                    ps.executeUpdate();
                }
            }
            long jaPersistidos = contarCheckIns(c, inscricao.getId());
            // Colunas origem/responsavel_id podem não existir em bancos pré-V7.
            boolean temOrigem = temColuna(c, "checkins", "origem");
            String insCheck = temOrigem
                    ? "INSERT INTO checkins (id, inscricao_id, atividade_id, data_hora, origem, responsavel_id)"
                            + " VALUES (?,?,?,?,?,?)"
                    : "INSERT INTO checkins (id, inscricao_id, atividade_id, data_hora)"
                            + " VALUES (?,?,?,?)";
            List<br.com.evento.domain.model.entity.CheckIn> todos = inscricao.getTodosCheckIns();
            for (int i = (int) jaPersistidos; i < todos.size(); i++) {
                var checkIn = todos.get(i);
                try (PreparedStatement ps = c.prepareStatement(insCheck)) {
                    ps.setString(1, java.util.UUID.randomUUID().toString());
                    ps.setString(2, inscricao.getId());
                    ps.setString(3, checkIn.getAtividadeId());
                    ps.setTimestamp(4, Timestamp.valueOf(checkIn.getDataHora()));
                    if (temOrigem) {
                        ps.setString(5, checkIn.getOrigem().name());
                        ps.setString(6, checkIn.getResponsavelId());
                    }
                    ps.executeUpdate();
                }
            }
            c.commit();
            return inscricao;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao salvar inscrição", e);
        }
    }

    @Override
    public Optional<Inscricao> buscarPorId(String id) {
        String sql = "SELECT id, participante_id, evento_id, data_inscricao FROM inscricoes WHERE id = ?";
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(reconstituir(c, rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar inscrição", e);
        }
    }

    @Override
    public List<Inscricao> buscarPorParticipante(String participanteId) {
        return buscar("SELECT id, participante_id, evento_id, data_inscricao FROM inscricoes"
                + " WHERE participante_id = ?", participanteId);
    }

    @Override
    public List<Inscricao> buscarPorEvento(String eventoId) {
        return buscar("SELECT id, participante_id, evento_id, data_inscricao FROM inscricoes"
                + " WHERE evento_id = ?", eventoId);
    }

    @Override
    public int contarPorEvento(String eventoId) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT count(*) FROM inscricoes WHERE evento_id = ?")) {
            ps.setString(1, eventoId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao contar inscrições", e);
        }
    }

    @Override
    public int contarPorAtividade(String eventoId, String atividadeId) {
        try (Connection c = factory.open();
              PreparedStatement ps = c.prepareStatement(
                      "SELECT count(*) FROM inscricao_atividades ia"
                              + " JOIN inscricoes i ON i.id = ia.inscricao_id"
                              + " WHERE i.evento_id = ? AND ia.atividade_id = ?")) {
            ps.setString(1, eventoId);
            ps.setString(2, atividadeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao contar seleções da atividade", e);
        }
    }

    @Override
    public List<InscricaoResumo> listarResumidoPorEvento(String eventoId) {
        return listarResumido(
                "SELECT i.id, i.participante_id, u.nome, i.evento_id, e.titulo, i.data_inscricao"
                        + " FROM inscricoes i JOIN usuarios u ON u.id = i.participante_id"
                        + " JOIN eventos e ON e.id = i.evento_id WHERE i.evento_id = ?"
                        + " ORDER BY i.data_inscricao",
                eventoId);
    }

    @Override
    public List<InscricaoResumo> listarResumidoPorParticipante(String participanteId) {
        return listarResumido(
                "SELECT i.id, i.participante_id, u.nome, i.evento_id, e.titulo, i.data_inscricao"
                        + " FROM inscricoes i JOIN usuarios u ON u.id = i.participante_id"
                        + " JOIN eventos e ON e.id = i.evento_id WHERE i.participante_id = ?"
                        + " ORDER BY i.data_inscricao",
                participanteId);
    }

    private List<InscricaoResumo> listarResumido(String sql, String param) {
        try (Connection c = factory.open();
              PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            List<InscricaoResumo> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new InscricaoResumo(
                            rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5),
                            rs.getTimestamp(6).toLocalDateTime()));
                }
            }
            return lista;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao listar inscrições", e);
        }
    }

    @Override
    public boolean existePorParticipanteEEvento(String participanteId, String eventoId) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM inscricoes WHERE participante_id = ? AND evento_id = ?")) {
            ps.setString(1, participanteId);
            ps.setString(2, eventoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao verificar inscrição", e);
        }
    }

    @Override
    public void remover(String id) {
        try (Connection c = factory.open()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM checkins WHERE inscricao_id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM inscricao_atividades WHERE inscricao_id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM inscricoes WHERE id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            c.commit();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao remover inscrição", e);
        }
    }

    private List<Inscricao> buscar(String sql, String param) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            List<Inscricao> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(reconstituir(c, rs));
            }
            return lista;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar inscrições", e);
        }
    }

    private Inscricao reconstituir(Connection c, ResultSet rs) throws Exception {
        Inscricao inscricao = new Inscricao(
                rs.getString("id"),
                rs.getString("participante_id"),
                rs.getString("evento_id"),
                rs.getTimestamp("data_inscricao").toLocalDateTime());
        String sqlAt = "SELECT a.id, a.titulo, a.inicio, a.fim, a.local FROM atividades a"
                + " JOIN inscricao_atividades ia ON ia.atividade_id = a.id"
                + " WHERE ia.inscricao_id = ? ORDER BY a.inicio";
        boolean temExtras = temColuna(c, "atividades", "tipo");
        if (temExtras) {
            sqlAt = "SELECT a.id, a.titulo, a.inicio, a.fim, a.local, a.tipo, a.trilha FROM atividades a"
                    + " JOIN inscricao_atividades ia ON ia.atividade_id = a.id"
                    + " WHERE ia.inscricao_id = ? ORDER BY a.inicio";
        }
        try (PreparedStatement ps = c.prepareStatement(sqlAt)) {
            ps.setString(1, inscricao.getId());
            try (ResultSet ra = ps.executeQuery()) {
                while (ra.next()) {
                    inscricao.selecionarAtividade(new Atividade(
                            ra.getString("id"),
                            ra.getString("titulo"),
                            new Periodo(ra.getTimestamp("inicio").toLocalDateTime(),
                                    ra.getTimestamp("fim").toLocalDateTime()),
                            ra.getString("local"),
                            temExtras ? ra.getString("tipo") : null,
                            temExtras ? ra.getString("trilha") : null));
                }
            }
        }
        boolean temOrigemCheck = temColuna(c, "checkins", "origem");
        String sqlCheck = temOrigemCheck
                ? "SELECT atividade_id, data_hora, origem, responsavel_id FROM checkins WHERE inscricao_id = ? ORDER BY data_hora"
                : "SELECT atividade_id, data_hora FROM checkins WHERE inscricao_id = ? ORDER BY data_hora";
        try (PreparedStatement ps = c.prepareStatement(sqlCheck)) {
            ps.setString(1, inscricao.getId());
            try (ResultSet rc = ps.executeQuery()) {
                while (rc.next()) {
                    String atividadeId = rc.getString("atividade_id");
                    inscricao.getAtividadesSelecionadas().stream()
                            .filter(a -> a.getId().equals(atividadeId))
                            .findFirst()
                            .ifPresent(a -> {
                                try {
                                    if (temOrigemCheck) {
                                        String origemRaw = rc.getString("origem");
                                        br.com.evento.domain.model.entity.CheckIn.Origem origem =
                                                "MANUAL".equalsIgnoreCase(origemRaw)
                                                        ? br.com.evento.domain.model.entity.CheckIn.Origem.MANUAL
                                                        : br.com.evento.domain.model.entity.CheckIn.Origem.QR_CODE;
                                        if (origem == br.com.evento.domain.model.entity.CheckIn.Origem.MANUAL) {
                                            inscricao.registrarCheckInManual(a,
                                                    rc.getTimestamp("data_hora").toLocalDateTime(),
                                                    rc.getString("responsavel_id"));
                                        } else {
                                            inscricao.registrarCheckIn(a,
                                                    rc.getTimestamp("data_hora").toLocalDateTime());
                                        }
                                    } else {
                                        inscricao.registrarCheckIn(a,
                                                rc.getTimestamp("data_hora").toLocalDateTime());
                                    }
                                } catch (Exception ignored) {
                                    // Órfão de atividade removida: ignora
                                }
                            });
                }
            }
        }
        return inscricao;
    }

    private long contarCheckIns(Connection c, String inscricaoId) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT count(*) FROM checkins WHERE inscricao_id = ?")) {
            ps.setString(1, inscricaoId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static boolean temColuna(Connection c, String tabela, String coluna) throws Exception {
        try (ResultSet rs = c.getMetaData().getColumns(null, null, tabela, coluna)) {
            return rs.next();
        }
    }

    /** IDs das atividades selecionadas (útil p/ relatórios). */
    public static List<String> idsAtividades(Inscricao i) {
        return i.getAtividadesSelecionadas().stream().map(Atividade::getId).collect(Collectors.toList());
    }
}
