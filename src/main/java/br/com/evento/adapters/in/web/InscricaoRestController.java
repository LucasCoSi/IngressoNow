package br.com.evento.adapters.in.web;

import br.com.evento.adapters.in.web.dto.InscreverRequest;
import br.com.evento.application.usecase.ConsultarAgendaUseCase;
import br.com.evento.application.usecase.InscreverParticipanteUseCase;
import br.com.evento.application.usecase.SelecionarAtividadeUseCase;
import br.com.evento.domain.model.entity.AgendaDoParticipante;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.port.out.InscricaoRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inscricoes")
public class InscricaoRestController {

    private final InscreverParticipanteUseCase inscreverParticipanteUseCase;
    private final ConsultarAgendaUseCase consultarAgendaUseCase;
    private final SelecionarAtividadeUseCase selecionarAtividadeUseCase;
    private final InscricaoRepository inscricaoRepository;

    public InscricaoRestController(InscreverParticipanteUseCase inscreverParticipanteUseCase,
                                   ConsultarAgendaUseCase consultarAgendaUseCase,
                                   SelecionarAtividadeUseCase selecionarAtividadeUseCase,
                                   InscricaoRepository inscricaoRepository) {
        this.inscreverParticipanteUseCase = inscreverParticipanteUseCase;
        this.consultarAgendaUseCase = consultarAgendaUseCase;
        this.selecionarAtividadeUseCase = selecionarAtividadeUseCase;
        this.inscricaoRepository = inscricaoRepository;
    }

    @GetMapping
    public ResponseEntity<List<InscricaoDTO>> listar(
            @RequestParam(required = false) String participanteId,
            @RequestParam(required = false) String eventoId) {
        List<Inscricao> inscricoes;
        if (participanteId != null && !participanteId.isBlank()) {
            inscricoes = inscricaoRepository.buscarPorParticipante(participanteId);
        } else {
            // eventoId é obrigatório para o filtro admin (MVP sem paginação global)
            inscricoes = inscricaoRepository.buscarPorEvento(eventoId != null ? eventoId : "");
        }
        List<InscricaoDTO> dtos = inscricoes.stream()
                .map(i -> new InscricaoDTO(i.getId(), i.getParticipanteId(), i.getEventoId(),
                        i.getDataInscricao().toString()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<InscricaoDTO> inscrever(@Valid @RequestBody InscreverRequest request) {
        Inscricao inscricao = inscreverParticipanteUseCase.executar(request.participanteId(), request.eventoId());
        return ResponseEntity.ok(new InscricaoDTO(inscricao.getId(), inscricao.getParticipanteId(),
                inscricao.getEventoId(), inscricao.getDataInscricao().toString()));
    }

    @PostMapping("/{inscricaoId}/atividades/{atividadeId}")
    public ResponseEntity<Void> selecionarAtividade(@PathVariable String inscricaoId, @PathVariable String atividadeId) {
        selecionarAtividadeUseCase.executar(inscricaoId, atividadeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{inscricaoId}/agenda")
    public ResponseEntity<AgendaDTO> consultarAgenda(@PathVariable String inscricaoId) {
        AgendaDoParticipante agenda = consultarAgendaUseCase.executar(inscricaoId);
        List<AtividadeDTO> atividades = agenda.getAtividades().stream()
                .map(a -> new AtividadeDTO(a.getId(), a.getTitulo(), a.getPeriodo().getInicio().toString(), a.getPeriodo().getFim().toString(), a.getLocal()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new AgendaDTO(agenda.getParticipanteId(), atividades));
    }

    public record InscricaoDTO(String id, String participanteId, String eventoId, String dataInscricao) {
        public InscricaoDTO(String id, String participanteId, String eventoId) {
            this(id, participanteId, eventoId, null);
        }
    }
    public record AtividadeDTO(String id, String titulo, String inicio, String fim, String local) {}
    public record AgendaDTO(String participanteId, List<AtividadeDTO> atividades) {}
}
