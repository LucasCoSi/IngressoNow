package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.port.out.EventoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Caso de uso: Adicionar uma Atividade a um Evento existente.
 * Busca o evento, delega a validação (conflito de horário/local) ao domínio, e salva.
 */
@Service
public class AdicionarAtividadeUseCase {

    private final EventoRepository eventoRepository;

    public AdicionarAtividadeUseCase(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    public Evento executar(String eventoId, String titulo, LocalDateTime inicio, LocalDateTime fim, String local) {
        Evento evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new DomainException("Evento não encontrado com id: " + eventoId));

        Periodo periodo = new Periodo(inicio, fim);
        Atividade atividade = new Atividade(titulo, periodo, local);

        // A regra de conflito de horário+local é responsabilidade da entidade Evento
        evento.adicionarAtividade(atividade);

        return eventoRepository.salvar(evento);
    }
}
