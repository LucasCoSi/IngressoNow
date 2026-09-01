package br.com.evento.adapters.in.web;

import br.com.evento.adapters.in.web.dto.CheckInRequest;
import br.com.evento.application.usecase.GerarRelatorioPresencaUseCase;
import br.com.evento.application.usecase.RealizarCheckInQrCodeUseCase;
import br.com.evento.domain.port.in.QrCodeService;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.port.out.InscricaoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/controle")
public class ControlePresencaRestController {

    private final RealizarCheckInQrCodeUseCase checkInUseCase;
    private final GerarRelatorioPresencaUseCase relatorioUseCase;
    private final QrCodeService qrCodeService;
    private final InscricaoRepository inscricaoRepository;

    public ControlePresencaRestController(RealizarCheckInQrCodeUseCase checkInUseCase,
                                          GerarRelatorioPresencaUseCase relatorioUseCase,
                                          QrCodeService qrCodeService,
                                          InscricaoRepository inscricaoRepository) {
        this.checkInUseCase = checkInUseCase;
        this.relatorioUseCase = relatorioUseCase;
        this.qrCodeService = qrCodeService;
        this.inscricaoRepository = inscricaoRepository;
    }

    @PostMapping("/checkin")
    public ResponseEntity<Void> realizarCheckIn(@Valid @RequestBody CheckInRequest request) {
        checkInUseCase.executar(request.qrCodeToken());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/qrcode/{inscricaoId}/{atividadeId}")
    public ResponseEntity<String> gerarQrCode(@PathVariable String inscricaoId, @PathVariable String atividadeId) {
        Inscricao inscricao = inscricaoRepository.buscarPorId(inscricaoId)
                .orElseThrow(() -> new RuntimeException("Inscricao nao encontrada"));
        String token = qrCodeService.gerarTokenParaCheckIn(inscricao, atividadeId);
        return ResponseEntity.ok(token);
    }

    @GetMapping(value = "/relatorio/presenca/{eventoId}", produces = "text/csv")
    public ResponseEntity<String> relatorioPresencaCsv(@PathVariable String eventoId) {
        String csv = relatorioUseCase.executarEmCsv(eventoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"presenca-" + eventoId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
