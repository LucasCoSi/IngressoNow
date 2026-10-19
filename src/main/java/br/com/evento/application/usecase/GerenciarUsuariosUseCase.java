package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Administrador;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Participante;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.UsuarioRepository;

import java.util.List;

/**
 * Ator Administrador (§3): gerencia usuários organizadores e consulta
 * todos os eventos. Organizador gerencia o operacional do evento.
 * Regra: não remove quem organiza evento existente, quem tem inscrições
 * (preservaria FKs) nem a si mesmo.
 */
public class GerenciarUsuariosUseCase {

    private final UsuarioRepository usuarioRepository;
    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;

    public GerenciarUsuariosUseCase(UsuarioRepository usuarioRepository,
                                    EventoRepository eventoRepository,
                                    InscricaoRepository inscricaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
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
        if (alvo instanceof Organizador || alvo instanceof Administrador) {
            boolean organiza = eventoRepository.buscarPorOrganizador(alvoId)
                    .stream().findAny().isPresent();
            if (organiza) {
                throw new DomainException(
                        "Usuário com eventos não pode ser removido.");
            }
        }
        // Participante com inscrições: a FK bloquearia no banco (erro 500).
        // A regra de negócio responde 422 com motivo antes de chegar lá.
        if (alvo instanceof Participante
                && !inscricaoRepository.buscarPorParticipante(alvoId).isEmpty()) {
            throw new DomainException(
                    "Usuário com inscrições não pode ser removido. Cancele-as antes.");
        }
        usuarioRepository.remover(alvoId);
    }
}
