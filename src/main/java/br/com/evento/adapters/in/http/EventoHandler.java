package br.com.evento.adapters.in.http;

import br.com.evento.application.usecase.AdicionarAtividadeUseCase;
import br.com.evento.application.usecase.CadastrarEventoUseCase;
import br.com.evento.application.usecase.ListarEventosPublicadosUseCase;
import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * /api/eventos — GET público; POST/PUT/DELETE exigem JWT.
 * Mesmos contratos do antigo EventoRestController.
 */
public class EventoHandler implements HttpHandler {

    private final ListarEventosPublicadosUseCase listar;
    private final CadastrarEventoUseCase cadastrar;
    private final AdicionarAtividadeUseCase adicionarAtividade;
    private final EventoRepository eventos;
    private final InscricaoRepository inscricoes;
    private final JwtAuth jwt;

    public EventoHandler(ListarEventosPublicadosUseCase listar, CadastrarEventoUseCase cadastrar,
                         AdicionarAtividadeUseCase adicionarAtividade, EventoRepository eventos,
                         InscricaoRepository inscricoes, JwtAuth jwt) {
        this.listar = listar;
        this.cadastrar = cadastrar;
        this.adicionarAtividade = adicionarAtividade;
        this.eventos = eventos;
        this.inscricoes = inscricoes;
        this.jwt = jwt;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String metodo = ex.getRequestMethod().toUpperCase();
            // /api/eventos + resto
            String resto = ex.getRequestURI().getPath().substring("/api/eventos".length());
            if (resto.isEmpty() || resto.equals("/")) {
                if ("GET".equals(metodo)) {
                    HttpErrors.json(ex, 200, listar.executar().stream().map(EventoDTO::from).toList());
                } else if ("POST".equals(metodo)) {
                    if (!jwt.exigir(ex)) return;
                    var req = Json.ler(HttpErrors.corpo(ex), CriarEventoRequest.class);
                    validarCriar(req);
                    Evento evento = cadastrar.executar(req.titulo(), req.descricao(), req.organizadorId());
                    if (req.maxVagas() != null) {
                        evento.configurarLimiteDeVagas(req.maxVagas());
                        evento = eventos.salvar(evento);
                    }
                    HttpErrors.json(ex, 201, EventoDTO.from(evento));
                } else {
                    HttpErrors.json(ex, 405, StandardError.de(405, "Method Not Allowed",
                            "Método não permitido.", ex.getRequestURI().getPath()));
                }
                return;
            }
            String[] partes = resto.substring(1).split("/");
            String id = partes[0];
            if (partes.length == 1) {
                if ("GET".equals(metodo)) {
                    HttpErrors.json(ex, 200, EventoDTO.from(buscar(id)));
                } else if ("PUT".equals(metodo)) {
                    if (!jwt.exigir(ex)) return;
                    var req = Json.ler(HttpErrors.corpo(ex), AtualizarEventoRequest.class);
                    Evento evento = buscar(id);
                    evento.atualizar(req.titulo(), req.descricao());
                    if (req.maxVagas() != null) evento.configurarLimiteDeVagas(req.maxVagas());
                    HttpErrors.json(ex, 200, EventoDTO.from(eventos.salvar(evento)));
                } else if ("DELETE".equals(metodo)) {
                    if (!jwt.exigir(ex)) return;
                    buscar(id);
                    if (inscricoes.contarPorEvento(id) > 0) {
                        throw new DomainException(
                                "Evento possui inscricoes e nao pode ser excluido. Encerre o evento em vez disso.");
                    }
                    eventos.remover(id);
                    HttpErrors.semConteudo(ex);
                } else {
                    HttpErrors.json(ex, 405, StandardError.de(405, "Method Not Allowed",
                            "Método não permitido.", ex.getRequestURI().getPath()));
                }
                return;
            }
            if (partes.length == 2 && "POST".equals(metodo)) {
                if (!jwt.exigir(ex)) return;
                switch (partes[1]) {
                    case "atividades" -> {
                        var req = Json.ler(HttpErrors.corpo(ex), CriarAtividadeRequest.class);
                        validarAtividade(req);
                        Evento evento = adicionarAtividade.executar(id, req.titulo(),
                                LocalDateTime.parse(req.inicio()), LocalDateTime.parse(req.fim()), req.local());
                        Atividade criada = evento.getAtividades().get(evento.getAtividades().size() - 1);
                        HttpErrors.json(ex, 201, AtividadeDTO.from(criada));
                    }
                    case "publicar" -> {
                        Evento evento = buscar(id);
                        evento.publicar();
                        HttpErrors.json(ex, 200, EventoDTO.from(eventos.salvar(evento)));
                    }
                    case "encerrar" -> {
                        Evento evento = buscar(id);
                        evento.encerrar();
                        HttpErrors.json(ex, 200, EventoDTO.from(eventos.salvar(evento)));
                    }
                    default -> HttpErrors.json(ex, 404, StandardError.de(404, "Not Found",
                            "Rota não encontrada.", ex.getRequestURI().getPath()));
                }
                return;
            }
            // GET /api/eventos/{id}/questionarios é tratado pelo AvaliacaoHandler via prefixo maior
            HttpErrors.json(ex, 404, StandardError.de(404, "Not Found",
                    "Rota não encontrada.", ex.getRequestURI().getPath()));
        } catch (Exception e) {
            HttpErrors.tratar(ex, e);
        }
    }

    private Evento buscar(String id) {
        return eventos.buscarPorId(id)
                .orElseThrow(() -> new DomainException("Evento nao encontrado com id: " + id));
    }

    private static void validarCriar(CriarEventoRequest req) {
        if (req.titulo() == null || req.titulo().isBlank() || req.titulo().length() < 3) {
            throw new IllegalArgumentException("O título deve ter ao menos 3 caracteres");
        }
        if (req.descricao() == null || req.descricao().isBlank() || req.descricao().length() < 10) {
            throw new IllegalArgumentException("A descrição deve ter ao menos 10 caracteres");
        }
        if (req.organizadorId() == null || req.organizadorId().isBlank()) {
            throw new IllegalArgumentException("O ID do organizador é obrigatório");
        }
        if (req.maxVagas() != null && req.maxVagas() <= 0) {
            throw new IllegalArgumentException("O número de vagas deve ser positivo");
        }
    }

    private static void validarAtividade(CriarAtividadeRequest req) {
        if (req.titulo() == null || req.titulo().isBlank() || req.titulo().length() < 3) {
            throw new IllegalArgumentException("O título deve ter ao menos 3 caracteres");
        }
        if (req.inicio() == null || req.fim() == null) {
            throw new IllegalArgumentException("Início e fim são obrigatórios");
        }
        if (req.local() == null || req.local().isBlank()) {
            throw new IllegalArgumentException("O local é obrigatório");
        }
    }

    public record CriarEventoRequest(String titulo, String descricao, String organizadorId, Integer maxVagas) {
    }

    public record AtualizarEventoRequest(String titulo, String descricao, Integer maxVagas) {
    }

    public record CriarAtividadeRequest(String titulo, String descricao, String inicio, String fim,
                                        String local, String eventoId, Integer maxVagas) {
    }

    public record EventoDTO(String id, String titulo, String descricao, String status,
                            Integer maxVagas, List<AtividadeDTO> atividades) {
        static EventoDTO from(Evento e) {
            return new EventoDTO(e.getId(), e.getTitulo(), e.getDescricao(),
                    e.getStatus().name(), e.getMaxVagas(),
                    e.getAtividades().stream().map(AtividadeDTO::from).toList());
        }
    }

    public record AtividadeDTO(String id, String titulo, String inicio, String fim, String local) {
        static AtividadeDTO from(Atividade a) {
            return new AtividadeDTO(a.getId(), a.getTitulo(),
                    a.getPeriodo().getInicio().toString(), a.getPeriodo().getFim().toString(), a.getLocal());
        }
    }
}
