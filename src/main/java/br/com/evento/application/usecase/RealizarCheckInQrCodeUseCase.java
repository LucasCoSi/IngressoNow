package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.port.in.DadosQrCode;
import br.com.evento.domain.port.in.QrCodeService;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RealizarCheckInQrCodeUseCase {

    private final QrCodeService qrCodeService;
    private final InscricaoRepository inscricaoRepository;
    private final EventoRepository eventoRepository;

    public RealizarCheckInQrCodeUseCase(QrCodeService qrCodeService, 
                                        InscricaoRepository inscricaoRepository,
                                        EventoRepository eventoRepository) {
        this.qrCodeService = qrCodeService;
        this.inscricaoRepository = inscricaoRepository;
        this.eventoRepository = eventoRepository;
    }

    public void executar(String qrCodeToken) {
        DadosQrCode dados = qrCodeService.decodificarToken(qrCodeToken);
        
        Inscricao inscricao = inscricaoRepository.buscarPorId(dados.inscricaoId())
                .orElseThrow(() -> new DomainException("Inscrição não encontrada."));
                
        Evento evento = eventoRepository.buscarPorId(inscricao.getEventoId())
                .orElseThrow(() -> new DomainException("Evento não encontrado."));
                
        Atividade atividade = evento.getAtividades().stream()
                .filter(a -> a.getId().equals(dados.atividadeId()))
                .findFirst()
                .orElseThrow(() -> new DomainException("Atividade não encontrada."));
                
        inscricao.registrarCheckIn(atividade, LocalDateTime.now());
        inscricaoRepository.salvar(inscricao);
    }
}
