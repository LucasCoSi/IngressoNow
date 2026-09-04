package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.StatusEvento;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import org.springframework.stereotype.Service;

@Service
public class CancelarInscricaoUseCase {

    private final InscricaoRepository inscricaoRepository;
    private final EventoRepository eventoRepository;

    public CancelarInscricaoUseCase(InscricaoRepository inscricaoRepository,
                                    EventoRepository eventoRepository) {
        this.inscricaoRepository = inscricaoRepository;
        this.eventoRepository = eventoRepository;
    }

    public void executar(String inscricaoId) {
        Inscricao inscricao = inscricaoRepository.buscarPorId(inscricaoId)
                .orElseThrow(() -> new DomainException("Inscrição não encontrada."));
        eventoRepository.buscarPorId(inscricao.getEventoId()).ifPresent(evento -> {
            if (evento.getStatus() == StatusEvento.ENCERRADO) {
                throw new DomainException("Inscrição de evento encerrado não pode ser cancelada.");
            }
        });
        inscricaoRepository.remover(inscricaoId);
    }
}
