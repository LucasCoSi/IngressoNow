package br.com.evento.adapters.out.jdbc;

import br.com.evento.domain.model.entity.Avaliacao;
import br.com.evento.domain.model.entity.Resposta;
import br.com.evento.domain.port.out.AvaliacaoRepository;
import br.com.evento.infra.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adapter JDBC puro da porta AvaliacaoRepository.
 */
public class JdbcAvaliacaoRepository implements AvaliacaoRepository {

    private final ConnectionFactory factory;

    public JdbcAvaliacaoRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Avaliacao salvar(Avaliacao avaliacao) {
        try (Connection c = factory.open()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO avaliacoes (id, questionario_id, inscricao_id, data_submissao)"
                            + " VALUES (?,?,?,?) ON CONFLICT (id) DO NOTHING")) {
                ps.setString(1, avaliacao.getId());
                ps.setString(2, avaliacao.getQuestionarioId());
                ps.setString(3, avaliacao.getInscricaoId());
                ps.setTimestamp(4, Timestamp.valueOf(avaliacao.getDataSubmissao()));
                ps.executeUpdate();
            }
            String ins = "INSERT INTO respostas (avaliacao_id, pergunta_id, valor) VALUES (?,?,?)"
                    + " ON CONFLICT (avaliacao_id, pergunta_id) DO UPDATE SET valor=EXCLUDED.valor";
            for (Resposta r : avaliacao.getRespostas()) {
                try (PreparedStatement ps = c.prepareStatement(ins)) {
                    ps.setString(1, avaliacao.getId());
                    ps.setString(2, r.getPerguntaId());
                    ps.setString(3, r.getValor());
                    ps.executeUpdate();
                }
            }
            c.commit();
            return avaliacao;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao salvar avaliação", e);
        }
    }

    @Override
    public Optional<Avaliacao> buscarPorId(String id) {
        String sql = "SELECT id, questionario_id, inscricao_id, data_submissao FROM avaliacoes WHERE id = ?";
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(reconstituir(c, rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar avaliação", e);
        }
    }

    @Override
    public List<Avaliacao> buscarPorQuestionario(String questionarioId) {
        String sql = "SELECT id, questionario_id, inscricao_id, data_submissao FROM avaliacoes"
                + " WHERE questionario_id = ?";
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, questionarioId);
            List<Avaliacao> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(reconstituir(c, rs));
            }
            return lista;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar avaliações", e);
        }
    }

    @Override
    public boolean existePorQuestionarioEInscricao(String questionarioId, String inscricaoId) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM avaliacoes WHERE questionario_id = ? AND inscricao_id = ?")) {
            ps.setString(1, questionarioId);
            ps.setString(2, inscricaoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao verificar avaliação", e);
        }
    }

    private Avaliacao reconstituir(Connection c, ResultSet rs) throws Exception {
        List<Resposta> respostas = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT pergunta_id, valor FROM respostas WHERE avaliacao_id = ?")) {
            ps.setString(1, rs.getString("id"));
            try (ResultSet rr = ps.executeQuery()) {
                while (rr.next()) respostas.add(new Resposta(rr.getString("pergunta_id"), rr.getString("valor")));
            }
        }
        return new Avaliacao(rs.getString("id"), rs.getString("questionario_id"),
                rs.getString("inscricao_id"), respostas,
                rs.getTimestamp("data_submissao").toLocalDateTime());
    }
}
