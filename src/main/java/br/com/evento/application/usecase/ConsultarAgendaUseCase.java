package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.AgendaDoParticipante;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.port.out.InscricaoRepository;

public class ConsultarAgendaUseCase {

    private final InscricaoRepository inscricaoRepository;

    public ConsultarAgendaUseCase(InscricaoRepository inscricaoRepository) {
        this.inscricaoRepository = inscricaoRepository;
    }

    public AgendaDoParticipante executar(String inscricaoId) {
        Inscricao inscricao = inscricaoRepository.buscarPorId(inscricaoId)
                .orElseThrow(() -> new DomainException("Inscrição não encontrada."));

        return inscricao.gerarAgenda();
    }
}
