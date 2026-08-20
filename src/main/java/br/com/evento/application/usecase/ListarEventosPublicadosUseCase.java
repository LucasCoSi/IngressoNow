package br.com.evento.application.usecase;

import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.StatusEvento;
import br.com.evento.domain.port.out.EventoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Caso de uso: Listar todos os eventos com status PUBLICADO ou INSCRICOES_ABERTAS.
 * Consumido pelo site público para exibição ao visitante.
 */
@Service
public class ListarEventosPublicadosUseCase {

    private final EventoRepository eventoRepository;

    public ListarEventosPublicadosUseCase(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    public List<Evento> executar() {
        List<Evento> publicados = eventoRepository.buscarPorStatus(StatusEvento.PUBLICADO);
        List<Evento> comInscricoes = eventoRepository.buscarPorStatus(StatusEvento.INSCRICOES_ABERTAS);

        publicados.addAll(comInscricoes);
        return publicados;
    }
}
