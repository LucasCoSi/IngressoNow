package br.com.evento.adapters.in.web;

import br.com.evento.application.usecase.ListarEventosPublicadosUseCase;
import br.com.evento.domain.model.entity.Evento;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/eventos")
public class EventoRestController {

    private final ListarEventosPublicadosUseCase listarEventosPublicadosUseCase;

    public EventoRestController(ListarEventosPublicadosUseCase listarEventosPublicadosUseCase) {
        this.listarEventosPublicadosUseCase = listarEventosPublicadosUseCase;
    }

    @GetMapping
    public ResponseEntity<List<EventoDTO>> listar() {
        List<Evento> eventos = listarEventosPublicadosUseCase.executar();
        List<EventoDTO> dtos = eventos.stream()
                .map(e -> new EventoDTO(e.getId(), e.getTitulo(), e.getDescricao(), e.getStatus().name()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    public record EventoDTO(String id, String titulo, String descricao, String status) {}
}
