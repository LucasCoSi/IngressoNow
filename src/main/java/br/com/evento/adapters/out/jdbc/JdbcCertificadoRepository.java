package br.com.evento.adapters.out.jdbc;

import br.com.evento.domain.model.entity.Certificado;
import br.com.evento.domain.model.entity.CriterioCertificado;
import br.com.evento.domain.port.out.CertificadoRepository;
import br.com.evento.infra.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RF-32–35 (D) — adapter JDBC puro da porta CertificadoRepository.
 */
public class JdbcCertificadoRepository implements CertificadoRepository {

    private final ConnectionFactory factory;

    public JdbcCertificadoRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public CriterioCertificado salvarCriterio(CriterioCertificado criterio) {
        try (Connection c = factory.open()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO criterios_certificado (id, evento_id, percentual_minimo)"
                            + " VALUES (?,?,?)"
                            + " ON CONFLICT (id) DO UPDATE SET percentual_minimo=EXCLUDED.percentual_minimo")) {
                ps.setString(1, criterio.getId());
                ps.setString(2, criterio.getEventoId());
                if (criterio.getPercentualMinimo() == null) ps.setNull(3, java.sql.Types.INTEGER);
                else ps.setInt(3, criterio.getPercentualMinimo());
                ps.executeUpdate();
            }
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM criterio_atividades WHERE criterio_id = ?")) {
                del.setString(1, criterio.getId());
                del.executeUpdate();
            }
            for (String atId : criterio.getAtividadesObrigatorias()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO criterio_atividades (criterio_id, atividade_id)"
                                + " VALUES (?,?) ON CONFLICT DO NOTHING")) {
                    ps.setString(1, criterio.getId());
                    ps.setString(2, atId);
                    ps.executeUpdate();
                }
            }
            c.commit();
            return criterio;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao salvar critério", e);
        }
    }

    @Override
    public Optional<CriterioCertificado> buscarCriterioPorEvento(String eventoId) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, evento_id, percentual_minimo FROM criterios_certificado"
                             + " WHERE evento_id = ?")) {
            ps.setString(1, eventoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                CriterioCertificado crit = new CriterioCertificado(
                        rs.getString("id"), rs.getString("evento_id"));
                Object perc = rs.getObject("percentual_minimo");
                crit.definirPercentualMinimo(perc == null ? null : ((Number) perc).intValue());
                try (PreparedStatement ps2 = c.prepareStatement(
                        "SELECT atividade_id FROM criterio_atividades WHERE criterio_id = ?")) {
                    ps2.setString(1, crit.getId());
                    try (ResultSet ra = ps2.executeQuery()) {
                        while (ra.next()) crit.exigirAtividade(ra.getString("atividade_id"));
                    }
                }
                return Optional.of(crit);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar critério", e);
        }
    }

    @Override
    public Certificado salvar(Certificado certificado) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO certificados (id, codigo, evento_id, participante_id,"
                             + " participante_nome, papel_declarado, carga_horaria, emitido_em)"
                             + " VALUES (?,?,?,?,?,?,?,?)"
                             + " ON CONFLICT (id) DO NOTHING")) {
            ps.setString(1, certificado.getId());
            ps.setString(2, certificado.getCodigo());
            ps.setString(3, certificado.getEventoId());
            ps.setString(4, certificado.getParticipanteId());
            ps.setString(5, certificado.getParticipanteNome());
            ps.setString(6, certificado.getPapelDeclarado());
            ps.setInt(7, certificado.getCargaHoraria());
            ps.setTimestamp(8, Timestamp.valueOf(certificado.getEmitidoEm()));
            ps.executeUpdate();
            return certificado;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao salvar certificado", e);
        }
    }

    @Override
    public Optional<Certificado> buscarPorId(String id) {
        try (Connection c = factory.open();
              PreparedStatement ps = c.prepareStatement(
                      "SELECT * FROM certificados WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapear(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar certificado", e);
        }
    }

    @Override
    public Optional<Certificado> buscarPorCodigo(String codigo) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM certificados WHERE codigo = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapear(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar certificado", e);
        }
    }

    @Override
    public List<Certificado> buscarPorEvento(String eventoId) {
        return listar("SELECT * FROM certificados WHERE evento_id = ? ORDER BY emitido_em", eventoId);
    }

    @Override
    public List<Certificado> buscarPorParticipante(String participanteId) {
        return listar("SELECT * FROM certificados WHERE participante_id = ? ORDER BY emitido_em",
                participanteId);
    }

    private List<Certificado> listar(String sql, String param) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            List<Certificado> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
            return lista;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao listar certificados", e);
        }
    }

    private static Certificado mapear(ResultSet rs) throws Exception {
        return new Certificado(rs.getString("id"), rs.getString("codigo"),
                rs.getString("evento_id"), rs.getString("participante_id"),
                rs.getString("participante_nome"), rs.getString("papel_declarado"),
                rs.getInt("carga_horaria"),
                rs.getTimestamp("emitido_em").toLocalDateTime());
    }
}
