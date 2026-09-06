package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.UsuarioRepository;

import java.time.LocalDateTime;

/**
 * RF-22 — lançamento manual alternativo ao QR, com autoria (RN-11/RN-12).
 * O responsável (organizador) é registrado no CheckIn.
 */
public class RealizarCheckInManualUseCase {

    private final InscricaoRepository inscricaoRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;

    public RealizarCheckInManualUseCase(InscricaoRepository inscricaoRepository,
                                        EventoRepository eventoRepository,
                                        UsuarioRepository usuarioRepository) {
        this.inscricaoRepository = inscricaoRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public void executar(String inscricaoId, String atividadeId, String responsavelId) {
        if (responsavelId == null || responsavelId.isBlank()) {
            throw new DomainException("O responsável pelo lançamento é obrigatório.");
        }
        usuarioRepository.buscarPorId(responsavelId)
                .orElseThrow(() -> new DomainException("Responsável não encontrado."));
        Inscricao inscricao = inscricaoRepository.buscarPorId(inscricaoId)
                .orElseThrow(() -> new DomainException("Inscrição não encontrada."));
        Evento evento = eventoRepository.buscarPorId(inscricao.getEventoId())
                .orElseThrow(() -> new DomainException("Evento não encontrado."));
        Atividade atividade = evento.getAtividades().stream()
                .filter(a -> a.getId().equals(atividadeId))
                .findFirst()
                .orElseThrow(() -> new DomainException("Atividade não encontrada."));
        inscricao.registrarCheckInManual(atividade, LocalDateTime.now(), responsavelId);
        inscricaoRepository.salvar(inscricao);
    }
}
