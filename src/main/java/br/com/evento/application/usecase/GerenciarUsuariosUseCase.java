package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.UsuarioRepository;

import java.util.List;

/**
 * Ator Administrador (§3): gerencia usuários organizadores e consulta
 * todos os eventos. Organizador gerencia o operacional do evento.
 * Regra: não remove quem organiza evento existente nem a si mesmo.
 */
public class GerenciarUsuariosUseCase {

    private final UsuarioRepository usuarioRepository;
    private final EventoRepository eventoRepository;

    public GerenciarUsuariosUseCase(UsuarioRepository usuarioRepository,
                                    EventoRepository eventoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.listarTodos();
    }

    public void remover(String solicitanteId, String alvoId) {
        if (solicitanteId.equals(alvoId)) {
            throw new DomainException("Você não pode remover a própria conta.");
        }
        Usuario alvo = usuarioRepository.buscarPorId(alvoId)
                .orElseThrow(() -> new DomainException("Usuário não encontrado."));
        if (alvo instanceof Organizador) {
            boolean organiza = eventoRepository.buscarPorOrganizador(alvoId)
                    .stream().findAny().isPresent();
            if (organiza) {
                throw new DomainException(
                        "Organizador com eventos não pode ser removido.");
            }
        }
        usuarioRepository.remover(alvoId);
    }
}
