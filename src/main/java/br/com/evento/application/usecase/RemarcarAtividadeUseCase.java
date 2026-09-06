package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.port.out.EventoRepository;

import java.time.LocalDateTime;

/**
 * RF-07 — remarcação de atividade com revalidação de conflito (RN-07).
 */
public class RemarcarAtividadeUseCase {

    private final EventoRepository eventoRepository;

    public RemarcarAtividadeUseCase(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    public void executar(String eventoId, String atividadeId,
                         LocalDateTime inicio, LocalDateTime fim, String local) {
        var evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new DomainException("Evento não encontrado com id: " + eventoId));
        evento.remarcarAtividade(atividadeId, new Periodo(inicio, fim), local);
        eventoRepository.salvar(evento);
    }
}
