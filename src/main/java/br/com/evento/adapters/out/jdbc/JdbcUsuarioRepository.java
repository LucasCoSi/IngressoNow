package br.com.evento.adapters.out.jdbc;

import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Participante;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.UsuarioRepository;
import br.com.evento.infra.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

/**
 * Adapter JDBC puro da porta UsuarioRepository (sem JPA/Hibernate).
 */
public class JdbcUsuarioRepository implements UsuarioRepository {

    private final ConnectionFactory factory;

    public JdbcUsuarioRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        String sql = "INSERT INTO usuarios (id, tipo_usuario, nome, email, senha_hash) VALUES (?,?,?,?,?)"
                + " ON CONFLICT (id) DO UPDATE SET nome=EXCLUDED.nome, email=EXCLUDED.email,"
                + " senha_hash=EXCLUDED.senha_hash, tipo_usuario=EXCLUDED.tipo_usuario";
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario.getId());
            ps.setString(2, usuario instanceof Organizador ? "ORGANIZADOR" : "PARTICIPANTE");
            ps.setString(3, usuario.getNome());
            ps.setString(4, usuario.getEmail().getEndereco());
            ps.setString(5, usuario.getSenha().getHash());
            ps.executeUpdate();
            return usuario;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao salvar usuário", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(String id) {
        String sql = "SELECT id, tipo_usuario, nome, email, senha_hash FROM usuarios WHERE id = ?";
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar usuário", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorEmail(Email email) {
        String sql = "SELECT id, tipo_usuario, nome, email, senha_hash FROM usuarios WHERE email = ?";
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email.getEndereco());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar usuário por e-mail", e);
        }
    }

    @Override
    public boolean existePorEmail(Email email) {
        return buscarPorEmail(email).isPresent();
    }

    @Override
    public java.util.List<Usuario> listarTodos() {
        String sql = "SELECT id, tipo_usuario, nome, email, senha_hash FROM usuarios ORDER BY nome";
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(sql)) {
            java.util.List<Usuario> lista = new java.util.ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
            return lista;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao listar usuários", e);
        }
    }

    @Override
    public void remover(String id) {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement("DELETE FROM usuarios WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao remover usuário", e);
        }
    }

    private static Usuario mapear(ResultSet rs) throws Exception {
        String id = rs.getString("id");
        String tipo = rs.getString("tipo_usuario");
        String nome = rs.getString("nome");
        Email email = new Email(rs.getString("email"));
        SenhaHash senha = new SenhaHash(rs.getString("senha_hash"));
        if ("ORGANIZADOR".equalsIgnoreCase(tipo)) {
            return new Organizador(id, nome, email, senha);
        }
        return new Participante(id, nome, email, senha);
    }
}
