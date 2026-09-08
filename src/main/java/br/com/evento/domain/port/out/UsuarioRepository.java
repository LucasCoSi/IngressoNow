package br.com.evento.domain.port.out;

import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.model.vo.Email;

import java.util.Optional;

/**
 * Porta de saída do domínio para persistência de Usuarios.
 * Nenhuma dependência de framework é permitida nesta interface.
 */
public interface UsuarioRepository {
    Usuario salvar(Usuario usuario);
    Optional<Usuario> buscarPorId(String id);
    Optional<Usuario> buscarPorEmail(Email email);
    boolean existePorEmail(Email email);
    /** Admin: lista todos (ordenado por nome). */
    java.util.List<Usuario> listarTodos();
    /** Admin: remove (bloqueado se é organizador de evento existente — ver use case). */
    void remover(String id);
}
