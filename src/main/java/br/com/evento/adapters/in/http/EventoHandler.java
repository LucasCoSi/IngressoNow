package br.com.evento.adapters.in.http;

import br.com.evento.application.usecase.AdicionarAtividadeUseCase;
import br.com.evento.application.usecase.CadastrarEventoUseCase;
import br.com.evento.application.usecase.ListarEventosPublicadosUseCase;
import br.com.evento.application.usecase.RemarcarAtividadeUseCase;
import br.com.evento.application.usecase.VincularPessoaUseCase;
import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Pessoa;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.PessoaRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * /api/eventos — GET público; POST/PUT/DELETE exigem JWT.
 * Mesmos contratos do antigo EventoRestController.
 */
public class EventoHandler implements HttpHandler {

    private final ListarEventosPublicadosUseCase listar;
    private final CadastrarEventoUseCase cadastrar;
    private final AdicionarAtividadeUseCase adicionarAtividade;
    private final RemarcarAtividadeUseCase remarcarAtividade;
    private final VincularPessoaUseCase vincularPessoa;
    private final EventoRepository eventos;
    private final InscricaoRepository inscricoes;
    private final PessoaRepository pessoas;
    private final JwtAuth jwt;

    public EventoHandler(ListarEventosPublicadosUseCase listar, CadastrarEventoUseCase cadastrar,
                         AdicionarAtividadeUseCase adicionarAtividade, RemarcarAtividadeUseCase remarcarAtividade,
                         VincularPessoaUseCase vincularPessoa, EventoRepository eventos,
                         InscricaoRepository inscricoes, PessoaRepository pessoas, JwtAuth jwt) {
        this.listar = listar;
        this.cadastrar = cadastrar;
        this.adicionarAtividade = adicionarAtividade;
        this.remarcarAtividade = remarcarAtividade;
        this.vincularPessoa = vincularPessoa;
        this.eventos = eventos;
        this.inscricoes = inscricoes;
        this.pessoas = pessoas;
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
                    aplicarDetalhes(evento, req);
                    if (req.maxVagas() != null) {
                        evento.configurarLimiteDeVagas(req.maxVagas());
                    }
                    evento = eventos.salvar(evento);
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
                    aplicarDetalhes(evento, req);
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
            // RF-09: GET /api/eventos/{id}/programacao com filtros server-side
            // (data, tipo, trilha, local — query params opcionais)
            if (partes.length == 2 && "programacao".equals(partes[1]) && "GET".equals(metodo)) {
                Evento evento = buscar(id);
                Map<String, String> params = InscricaoHandler.query(ex.getRequestURI().getRawQuery());
                var lista = evento.getAtividades().stream()
                        .filter(a -> programa(params.get("data"), a))
                        .filter(a -> matchParam(params.get("tipo"), a.getTipo()))
                        .filter(a -> matchParam(params.get("trilha"), a.getTrilha()))
                        .filter(a -> matchParam(params.get("local"), a.getLocal()))
                        .map(a -> AtividadeDTO.from(a, pessoas.buscarPorAtividade(a.getId()).stream()
                                .map(PessoaDTO::from).toList()))
                        .toList();
                HttpErrors.json(ex, 200, lista);
                return;
            }
            if (partes.length == 2 && "POST".equals(metodo)) {
                if (!jwt.exigir(ex)) return;
                switch (partes[1]) {
                    case "atividades" -> {
                        var req = Json.ler(HttpErrors.corpo(ex), CriarAtividadeRequest.class);
                        validarAtividade(req);
                        Evento evento = adicionarAtividade.executar(id, req.titulo(),
                                LocalDateTime.parse(req.inicio()), LocalDateTime.parse(req.fim()), req.local(),
                                req.tipo(), req.trilha(), req.maxVagas());
                        Atividade criada = evento.getAtividades().get(evento.getAtividades().size() - 1);
                        // RF-19: critério de presença opcional (UNICA default)
                        if (req.politicaFrequencia() != null && !req.politicaFrequencia().isBlank()) {
                            criada.definirPoliticaFrequencia(req.politicaFrequencia());
                            Evento salvo = eventos.salvar(evento);
                            String criadaId = criada.getId();
                            criada = salvo.getAtividades().stream()
                                    .filter(a -> a.getId().equals(criadaId)).findFirst().orElse(criada);
                        }
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
            // RF-07: PUT /api/eventos/{id}/atividades/{atId} (remarcar com revalidação)
            if (partes.length == 3 && "atividades".equals(partes[1]) && "PUT".equals(metodo)) {
                if (!jwt.exigir(ex)) return;
                var req = Json.ler(HttpErrors.corpo(ex), RemarcarRequest.class);
                if (req.inicio() == null || req.fim() == null) {
                    throw new IllegalArgumentException("Início e fim são obrigatórios");
                }
                if (req.local() == null || req.local().isBlank()) {
                    throw new IllegalArgumentException("O local é obrigatório");
                }
                remarcarAtividade.executar(id, partes[2],
                        LocalDateTime.parse(req.inicio()), LocalDateTime.parse(req.fim()), req.local());
                HttpErrors.json(ex, 200, AtividadeDTO.from(buscar(id).getAtividades().stream()
                        .filter(a -> a.getId().equals(partes[2])).findFirst()
                        .orElseThrow(() -> new DomainException("Atividade não encontrada."))));
                return;
            }
            // RF-08: POST/GET /api/eventos/{id}/atividades/{atId}/pessoas + DELETE /pessoas/{pId}
            if (partes.length == 4 && "atividades".equals(partes[1]) && "pessoas".equals(partes[3])) {
                if ("GET".equals(metodo)) {
                    HttpErrors.json(ex, 200, pessoas.buscarPorAtividade(partes[2]).stream()
                            .map(PessoaDTO::from).toList());
                    return;
                }
                if ("POST".equals(metodo)) {
                    if (!jwt.exigir(ex)) return;
                    var req = Json.ler(HttpErrors.corpo(ex), VincularPessoaRequest.class);
                    if (req.nome() == null || req.nome().isBlank()) {
                        throw new IllegalArgumentException("O nome da pessoa é obrigatório");
                    }
                    if (req.papel() == null || req.papel().isBlank()) {
                        throw new IllegalArgumentException("O papel da pessoa é obrigatório");
                    }
                    HttpErrors.json(ex, 201,
                            PessoaDTO.from(vincularPessoa.executar(id, partes[2], req.nome(), req.papel())));
                    return;
                }
            }
            if (partes.length == 3 && "pessoas".equals(partes[1]) && "DELETE".equals(metodo)) {
                if (!jwt.exigir(ex)) return;
                vincularPessoa.remover(partes[2]);
                HttpErrors.semConteudo(ex);
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
        if (req.maxVagas() != null && req.maxVagas() <= 0) {
            throw new IllegalArgumentException("O número de vagas da atividade deve ser positivo");
        }
    }

    public record DetalhesEvento(String periodoInicio, String periodoFim, String local, String fuso,
                                     Boolean exigeEscolhaAtividades, Boolean vagasPorAtividade) {
    }

    public record CriarEventoRequest(String titulo, String descricao, String organizadorId, Integer maxVagas,
                                     String periodoInicio, String periodoFim, String local, String fuso,
                                     Boolean exigeEscolhaAtividades, Boolean vagasPorAtividade) {
    }

    public record AtualizarEventoRequest(String titulo, String descricao, Integer maxVagas,
                                         String periodoInicio, String periodoFim, String local, String fuso,
                                         Boolean exigeEscolhaAtividades, Boolean vagasPorAtividade) {
    }

    /** RF-04/RF-13/RN-20: aplica detalhes opcionais (período, local, fuso, regras). */
    private static void aplicarDetalhes(Evento evento,
                                        String periodoInicio, String periodoFim, String local, String fuso,
                                        Boolean exigeEscolha, Boolean vagasPorAtividade) {
        br.com.evento.domain.model.vo.Periodo periodo = null;
        if (periodoInicio != null && !periodoInicio.isBlank()
                && periodoFim != null && !periodoFim.isBlank()) {
            periodo = new br.com.evento.domain.model.vo.Periodo(
                    LocalDateTime.parse(periodoInicio.trim()),
                    LocalDateTime.parse(periodoFim.trim()));
        }
        if (periodo != null || (local != null && !local.isBlank())) {
            evento.definirDetalhes(periodo, local);
        }
        if (fuso != null && !fuso.isBlank()) evento.definirFuso(fuso);
        if (exigeEscolha != null || vagasPorAtividade != null) {
            evento.configurarInscricao(
                    exigeEscolha != null && exigeEscolha,
                    vagasPorAtividade != null && vagasPorAtividade);
        }
    }

    private static void aplicarDetalhes(Evento evento, CriarEventoRequest req) {
        aplicarDetalhes(evento, req.periodoInicio(), req.periodoFim(), req.local(), req.fuso(),
                req.exigeEscolhaAtividades(), req.vagasPorAtividade());
    }

    private static void aplicarDetalhes(Evento evento, AtualizarEventoRequest req) {
        aplicarDetalhes(evento, req.periodoInicio(), req.periodoFim(), req.local(), req.fuso(),
                req.exigeEscolhaAtividades(), req.vagasPorAtividade());
    }

    public record CriarAtividadeRequest(String titulo, String descricao, String inicio, String fim,
                                        String local, String eventoId, Integer maxVagas,
                                        String tipo, String trilha, String politicaFrequencia) {
    }

    public record EventoDTO(String id, String titulo, String descricao, String status,
                            Integer maxVagas, List<AtividadeDTO> atividades,
                            String periodoInicio, String periodoFim, String local, String fuso,
                            boolean exigeEscolhaAtividades, boolean vagasPorAtividade) {
        static EventoDTO from(Evento e) {
            return new EventoDTO(e.getId(), e.getTitulo(), e.getDescricao(),
                    e.getStatus().name(), e.getMaxVagas(),
                    e.getAtividades().stream().map(AtividadeDTO::from).toList(),
                    e.getPeriodo() != null ? e.getPeriodo().getInicio().toString() : null,
                    e.getPeriodo() != null ? e.getPeriodo().getFim().toString() : null,
                    e.getLocal(), e.getFuso(),
                    e.isExigeEscolhaAtividades(), e.isVagasPorAtividade());
        }
    }

    public record AtividadeDTO(String id, String titulo, String inicio, String fim, String local,
                               String tipo, String trilha, String politicaFrequencia, Integer maxVagas) {
        static AtividadeDTO from(Atividade a) {
            return new AtividadeDTO(a.getId(), a.getTitulo(),
                    a.getPeriodo().getInicio().toString(), a.getPeriodo().getFim().toString(), a.getLocal(),
                    a.getTipo(), a.getTrilha(), a.getNomePoliticaFrequencia(), a.getMaxVagas());
        }

        static AtividadeDTO from(Atividade a, List<PessoaDTO> pessoas) {
            return from(a);
        }
    }

    public record RemarcarRequest(String inicio, String fim, String local) {
    }

    public record VincularPessoaRequest(String nome, String papel) {
    }

    public record PessoaDTO(String id, String atividadeId, String nome, String papel) {
        static PessoaDTO from(Pessoa p) {
            return new PessoaDTO(p.getId(), p.getAtividadeId(), p.getNome(), p.getPapel());
        }
    }

    /** RF-09: match case-insensitive de filtro opcional (vazio = aceita tudo). */
    private static boolean matchParam(String filtro, String valor) {
        if (filtro == null || filtro.isBlank()) return true;
        if (valor == null) return false;
        return valor.equalsIgnoreCase(filtro.trim());
    }

    /** RF-09: filtro por data (yyyy-MM-dd) compara o dia do início da atividade. */
    private static boolean programa(String data, Atividade a) {
        if (data == null || data.isBlank()) return true;
        try {
            java.time.LocalDate dia = java.time.LocalDate.parse(data.trim());
            return a.getPeriodo().getInicio().toLocalDate().equals(dia);
        } catch (Exception e) {
            throw new IllegalArgumentException("Filtro de data inválido (use yyyy-MM-dd).");
        }
    }
}
