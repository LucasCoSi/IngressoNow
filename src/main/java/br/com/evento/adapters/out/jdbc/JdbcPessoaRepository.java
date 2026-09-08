package br.com.evento.adapters.out.jdbc;

import br.com.evento.domain.model.entity.Pessoa;
import br.com.evento.domain.port.out.PessoaRepository;
import br.com.evento.infra.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * RF-08 — adapter JDBC puro da porta PessoaRepository.
 */
public class JdbcPessoaRepository implements PessoaRepository {

    private final ConnectionFactory factory;

    public JdbcPessoaRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Pessoa salvar(Pessoa pessoa) {
        String sql = "INSERT INTO pessoas (id, atividade_id, nome, papel) VALUES (?,?,?,?)"
                + " ON CONFLICT (id) DO UPDATE SET nome=EXCLUDED.nome, papel=EXCLUDED.papel,"
                + " atividade_id=EXCLUDED.atividade_id";
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pessoa.getId());
            ps.setString(2, pessoa.getAtividadeId());
            ps.setString(3, pessoa.getNome());
            ps.setString(4, pessoa.getPapel());
            ps.executeUpdate();
            return pessoa;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao salvar pessoa", e);
        }
    }

    @Override
    public Optional<Pessoa> buscarPorId(String id) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, atividade_id, nome, papel FROM pessoas WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar pessoa", e);
        }
    }

    @Override
    public List<Pessoa> buscarPorAtividade(String atividadeId) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, atividade_id, nome, papel FROM pessoas WHERE atividade_id = ? ORDER BY nome")) {
            ps.setString(1, atividadeId);
            List<Pessoa> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
            return lista;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar pessoas da atividade", e);
        }
    }

    @Override
    public void remover(String id) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement("DELETE FROM pessoas WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao remover pessoa", e);
        }
    }

    private static Pessoa mapear(ResultSet rs) throws Exception {
        return new Pessoa(rs.getString("id"), rs.getString("atividade_id"),
                rs.getString("nome"), rs.getString("papel"));
    }
}
