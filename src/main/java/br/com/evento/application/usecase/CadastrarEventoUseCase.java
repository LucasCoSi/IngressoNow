package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.UsuarioRepository;

/**
 * Caso de uso: Cadastrar um novo Evento em estado RASCUNHO.
 * Orquestra a busca do organizador e delega a criação ao domínio.
 */
public class CadastrarEventoUseCase {

    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;

    public CadastrarEventoUseCase(EventoRepository eventoRepository, UsuarioRepository usuarioRepository) {
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Evento executar(String titulo, String descricao, String organizadorId) {
        Usuario usuario = usuarioRepository.buscarPorId(organizadorId)
                .orElseThrow(() -> new DomainException("Organizador não encontrado com id: " + organizadorId));

        if (!(usuario instanceof Organizador organizador)) {
            throw new DomainException("Apenas organizadores podem criar eventos.");
        }

        Evento evento = new Evento(titulo, descricao, organizador);
        return eventoRepository.salvar(evento);
    }
}
