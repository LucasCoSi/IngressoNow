package br.com.evento.adapters.in.web;

import br.com.evento.adapters.in.web.dto.AtualizarEventoRequest;
import br.com.evento.adapters.in.web.dto.CriarAtividadeRequest;
import br.com.evento.adapters.in.web.dto.CriarEventoRequest;
import br.com.evento.application.usecase.AdicionarAtividadeUseCase;
import br.com.evento.application.usecase.CadastrarEventoUseCase;
import br.com.evento.application.usecase.ListarEventosPublicadosUseCase;
import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/eventos")
public class EventoRestController {

    private final ListarEventosPublicadosUseCase listarEventosPublicadosUseCase;
    private final CadastrarEventoUseCase cadastrarEventoUseCase;
    private final AdicionarAtividadeUseCase adicionarAtividadeUseCase;
    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;

    public EventoRestController(ListarEventosPublicadosUseCase listarEventosPublicadosUseCase,
                                CadastrarEventoUseCase cadastrarEventoUseCase,
                                AdicionarAtividadeUseCase adicionarAtividadeUseCase,
                                EventoRepository eventoRepository,
                                InscricaoRepository inscricaoRepository) {
        this.listarEventosPublicadosUseCase = listarEventosPublicadosUseCase;
        this.cadastrarEventoUseCase = cadastrarEventoUseCase;
        this.adicionarAtividadeUseCase = adicionarAtividadeUseCase;
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
    }

    @GetMapping
    public ResponseEntity<List<EventoDTO>> listar() {
        List<Evento> eventos = listarEventosPublicadosUseCase.executar();
        List<EventoDTO> dtos = eventos.stream()
                .map(EventoDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoDTO> buscarPorId(@PathVariable String id) {
        Evento evento = eventoRepository.buscarPorId(id)
                .orElseThrow(() -> new DomainException("Evento nao encontrado com id: " + id));
        return ResponseEntity.ok(EventoDTO.from(evento));
    }

    @PostMapping
    public ResponseEntity<EventoDTO> criar(@Valid @RequestBody CriarEventoRequest request) {
        Evento evento = cadastrarEventoUseCase.executar(
                request.titulo(), request.descricao(), request.organizadorId());
        if (request.maxVagas() != null) {
            evento.configurarLimiteDeVagas(request.maxVagas());
            evento = eventoRepository.salvar(evento);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(EventoDTO.from(evento));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoDTO> atualizar(@PathVariable String id,
                                               @Valid @RequestBody AtualizarEventoRequest request) {
        Evento evento = eventoRepository.buscarPorId(id)
                .orElseThrow(() -> new DomainException("Evento nao encontrado com id: " + id));
        evento.atualizar(request.titulo(), request.descricao());
        if (request.maxVagas() != null) {
            evento.configurarLimiteDeVagas(request.maxVagas());
        }
        return ResponseEntity.ok(EventoDTO.from(eventoRepository.salvar(evento)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable String id) {
        eventoRepository.buscarPorId(id)
                .orElseThrow(() -> new DomainException("Evento nao encontrado com id: " + id));
        if (inscricaoRepository.contarPorEvento(id) > 0) {
            throw new DomainException("Evento possui inscricoes e nao pode ser excluido. Encerre o evento em vez disso.");
        }
        eventoRepository.remover(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/atividades")
    public ResponseEntity<AtividadeDTO> adicionarAtividade(@PathVariable String id,
                                                          @Valid @RequestBody CriarAtividadeRequest request) {
        Evento evento = adicionarAtividadeUseCase.executar(
                id, request.titulo(), request.inicio(), request.fim(), request.local());
        Atividade criada = evento.getAtividades().get(evento.getAtividades().size() - 1);
        return ResponseEntity.status(HttpStatus.CREATED).body(AtividadeDTO.from(criada));
    }

    @PostMapping("/{id}/publicar")
    public ResponseEntity<EventoDTO> publicar(@PathVariable String id) {
        Evento evento = eventoRepository.buscarPorId(id)
                .orElseThrow(() -> new DomainException("Evento nao encontrado com id: " + id));
        evento.publicar();
        return ResponseEntity.ok(EventoDTO.from(eventoRepository.salvar(evento)));
    }

    @PostMapping("/{id}/encerrar")
    public ResponseEntity<EventoDTO> encerrar(@PathVariable String id) {
        Evento evento = eventoRepository.buscarPorId(id)
                .orElseThrow(() -> new DomainException("Evento nao encontrado com id: " + id));
        evento.encerrar();
        return ResponseEntity.ok(EventoDTO.from(eventoRepository.salvar(evento)));
    }

    public record EventoDTO(String id, String titulo, String descricao, String status,
                            Integer maxVagas, List<AtividadeDTO> atividades) {
        static EventoDTO from(Evento e) {
            List<AtividadeDTO> atividades = e.getAtividades().stream()
                    .map(AtividadeDTO::from)
                    .collect(Collectors.toList());
            return new EventoDTO(e.getId(), e.getTitulo(), e.getDescricao(),
                    e.getStatus().name(), e.getMaxVagas(), atividades);
        }
    }

    public record AtividadeDTO(String id, String titulo, String inicio, String fim, String local) {
        static AtividadeDTO from(Atividade a) {
            return new AtividadeDTO(a.getId(), a.getTitulo(),
                    a.getPeriodo().getInicio().toString(),
                    a.getPeriodo().getFim().toString(), a.getLocal());
        }
    }
}
