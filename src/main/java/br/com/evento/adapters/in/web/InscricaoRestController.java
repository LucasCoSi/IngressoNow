package br.com.evento.adapters.in.web;

import br.com.evento.adapters.in.web.dto.InscreverRequest;
import br.com.evento.application.usecase.CancelarInscricaoUseCase;
import br.com.evento.application.usecase.ConsultarAgendaUseCase;
import br.com.evento.application.usecase.InscreverParticipanteUseCase;
import br.com.evento.application.usecase.SelecionarAtividadeUseCase;
import br.com.evento.domain.model.entity.AgendaDoParticipante;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.UsuarioRepository;
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
    private final CancelarInscricaoUseCase cancelarInscricaoUseCase;
    private final InscricaoRepository inscricaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EventoRepository eventoRepository;

    public InscricaoRestController(InscreverParticipanteUseCase inscreverParticipanteUseCase,
                                   ConsultarAgendaUseCase consultarAgendaUseCase,
                                   SelecionarAtividadeUseCase selecionarAtividadeUseCase,
                                   CancelarInscricaoUseCase cancelarInscricaoUseCase,
                                   InscricaoRepository inscricaoRepository,
                                   UsuarioRepository usuarioRepository,
                                   EventoRepository eventoRepository) {
        this.inscreverParticipanteUseCase = inscreverParticipanteUseCase;
        this.consultarAgendaUseCase = consultarAgendaUseCase;
        this.selecionarAtividadeUseCase = selecionarAtividadeUseCase;
        this.cancelarInscricaoUseCase = cancelarInscricaoUseCase;
        this.inscricaoRepository = inscricaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
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
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<InscricaoDTO> inscrever(@Valid @RequestBody InscreverRequest request) {
        Inscricao inscricao = inscreverParticipanteUseCase.executar(request.participanteId(), request.eventoId());
        return ResponseEntity.ok(toDTO(inscricao));
    }

    private InscricaoDTO toDTO(Inscricao i) {
        String participanteNome = usuarioRepository.buscarPorId(i.getParticipanteId())
                .map(u -> u.getNome()).orElse(i.getParticipanteId());
        String eventoTitulo = eventoRepository.buscarPorId(i.getEventoId())
                .map(e -> e.getTitulo()).orElse(i.getEventoId());
        return new InscricaoDTO(i.getId(), i.getParticipanteId(), participanteNome,
                i.getEventoId(), eventoTitulo, i.getDataInscricao().toString());
    }

    @DeleteMapping("/{inscricaoId}")
    public ResponseEntity<Void> cancelar(@PathVariable String inscricaoId) {
        cancelarInscricaoUseCase.executar(inscricaoId);
        return ResponseEntity.noContent().build();
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

    public record InscricaoDTO(String id, String participanteId, String participanteNome,
                               String eventoId, String eventoTitulo, String dataInscricao) {
        public InscricaoDTO(String id, String participanteId, String eventoId) {
            this(id, participanteId, null, eventoId, null, null);
        }
    }
    public record AtividadeDTO(String id, String titulo, String inicio, String fim, String local) {}
    public record AgendaDTO(String participanteId, List<AtividadeDTO> atividades) {}
}
