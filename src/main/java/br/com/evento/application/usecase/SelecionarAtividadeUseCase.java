package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;

public class SelecionarAtividadeUseCase {

    private final InscricaoRepository inscricaoRepository;
    private final EventoRepository eventoRepository;

    public SelecionarAtividadeUseCase(InscricaoRepository inscricaoRepository, EventoRepository eventoRepository) {
        this.inscricaoRepository = inscricaoRepository;
        this.eventoRepository = eventoRepository;
    }

    public Inscricao executar(String inscricaoId, String atividadeId) {
        Inscricao inscricao = inscricaoRepository.buscarPorId(inscricaoId)
                .orElseThrow(() -> new DomainException("Inscrição não encontrada."));

        Evento evento = eventoRepository.buscarPorId(inscricao.getEventoId())
                .orElseThrow(() -> new DomainException("Evento não encontrado."));

        Atividade atividade = evento.getAtividades().stream()
                .filter(a -> a.getId().equals(atividadeId))
                .findFirst()
                .orElseThrow(() -> new DomainException("Atividade não pertence a este evento."));

        // RF-13/RN-06: vagas por atividade (só quando o evento ativa a regra
        // e a atividade tem limite). Duplicada cai no selecionarAtividade abaixo.
        boolean jaSelecionada = inscricao.getAtividadesSelecionadas().stream()
                .anyMatch(a -> a.getId().equals(atividadeId));
        if (!jaSelecionada && evento.isVagasPorAtividade() && atividade.getMaxVagas() != null) {
            int ocupadas = inscricaoRepository.contarPorAtividade(inscricao.getEventoId(), atividadeId);
            if (!atividade.podeAcomodar(ocupadas)) {
                throw new DomainException("A atividade '" + atividade.getTitulo()
                        + "' não possui mais vagas disponíveis.");
            }
        }

        inscricao.selecionarAtividade(atividade);
        return inscricaoRepository.salvar(inscricao);
    }
}
