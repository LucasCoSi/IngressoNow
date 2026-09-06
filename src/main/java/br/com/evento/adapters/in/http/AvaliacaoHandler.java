package br.com.evento.adapters.in.http;

import br.com.evento.application.usecase.ConsolidarAvaliacoesUseCase;
import br.com.evento.application.usecase.CriarQuestionarioUseCase;
import br.com.evento.application.usecase.SubmeterAvaliacaoUseCase;
import br.com.evento.domain.model.entity.Avaliacao;
import br.com.evento.domain.model.entity.Pergunta;
import br.com.evento.domain.model.entity.Questionario;
import br.com.evento.domain.model.entity.Resposta;
import br.com.evento.domain.model.entity.TipoPergunta;
import br.com.evento.domain.port.out.QuestionarioRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

/**
 * Questionários e avaliações (RF-24..RF-28).
 * Rotas: POST/GET /api/eventos/{id}/questionarios,
 * POST /api/avaliacoes, GET /api/questionarios/{id}/resultados.
 * Tudo exige JWT (o GET de eventos continua público no EventoHandler).
 */
public class AvaliacaoHandler implements HttpHandler {

    private final CriarQuestionarioUseCase criar;
    private final SubmeterAvaliacaoUseCase submeter;
    private final ConsolidarAvaliacoesUseCase consolidar;
    private final QuestionarioRepository questionarios;
    private final JwtAuth jwt;

    public AvaliacaoHandler(CriarQuestionarioUseCase criar, SubmeterAvaliacaoUseCase submeter,
                            ConsolidarAvaliacoesUseCase consolidar,
                            QuestionarioRepository questionarios, JwtAuth jwt) {
        this.criar = criar;
        this.submeter = submeter;
        this.consolidar = consolidar;
        this.questionarios = questionarios;
        this.jwt = jwt;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            if (!jwt.exigir(ex)) return;
            String metodo = ex.getRequestMethod().toUpperCase();
            String path = ex.getRequestURI().getPath();
            if (path.startsWith("/api/eventos/") && path.endsWith("/questionarios")) {
                String eventoId = path.substring("/api/eventos/".length(),
                        path.length() - "/questionarios".length());
                eventoId = eventoId.replace("/", "");
                if ("POST".equals(metodo)) {
                    var req = Json.ler(HttpErrors.corpo(ex), CriarQuestionarioRequest.class);
                    if (req.perguntas() == null || req.perguntas().isEmpty()) {
                        throw new IllegalArgumentException("O questionário exige ao menos uma pergunta");
                    }
                    List<Pergunta> perguntas = req.perguntas().stream()
                            .map(p -> new Pergunta(null, p.enunciado(), p.tipo(),
                                    p.obrigatoria() != null ? p.obrigatoria() : true,
                                    p.opcoes(), p.escalaMax()))
                            .toList();
                    HttpErrors.json(ex, 201,
                            QuestionarioDTO.from(criar.executar(eventoId, req.titulo(), perguntas,
                                    req.politicaIdentificacao())));
                } else if ("GET".equals(metodo)) {
                    HttpErrors.json(ex, 200, questionarios.buscarPorEvento(eventoId).stream()
                            .map(QuestionarioDTO::from).toList());
                } else {
                    HttpErrors.json(ex, 405, StandardError.de(405, "Method Not Allowed",
                            "Método não permitido.", path));
                }
                return;
            }
            if (path.equals("/api/avaliacoes") && "POST".equals(metodo)) {
                var req = Json.ler(HttpErrors.corpo(ex), SubmeterRequest.class);
                if (req.questionarioId() == null || req.questionarioId().isBlank()) {
                    throw new IllegalArgumentException("O questionário é obrigatório");
                }
                if (req.inscricaoId() == null || req.inscricaoId().isBlank()) {
                    throw new IllegalArgumentException("A inscrição é obrigatória");
                }
                if (req.respostas() == null || req.respostas().isEmpty()) {
                    throw new IllegalArgumentException("A avaliação exige ao menos uma resposta");
                }
                List<Resposta> respostas = req.respostas().stream()
                        .map(r -> new Resposta(r.perguntaId(), r.valor())).toList();
                Avaliacao a = submeter.executar(req.questionarioId(), req.inscricaoId(), respostas);
                HttpErrors.json(ex, 201, AvaliacaoDTO.from(a));
                return;
            }
            if (path.startsWith("/api/questionarios/") && path.endsWith("/resultados") && "GET".equals(metodo)) {
                String id = path.substring("/api/questionarios/".length(),
                        path.length() - "/resultados".length()).replace("/", "");
                HttpErrors.json(ex, 200, consolidar.executar(id));
                return;
            }
            HttpErrors.json(ex, 404, StandardError.de(404, "Not Found",
                    "Rota não encontrada.", path));
        } catch (Exception e) {
            HttpErrors.tratar(ex, e);
        }
    }

    public record CriarQuestionarioRequest(String titulo, List<PerguntaRequest> perguntas,
                                           String politicaIdentificacao) {
    }

    public record PerguntaRequest(String enunciado, TipoPergunta tipo, Boolean obrigatoria,
                                  List<String> opcoes, Integer escalaMax) {
    }

    public record SubmeterRequest(String questionarioId, String inscricaoId, List<RespostaRequest> respostas) {
    }

    public record RespostaRequest(String perguntaId, String valor) {
    }

    public record PerguntaDTO(String id, String enunciado, TipoPergunta tipo, boolean obrigatoria,
                              List<String> opcoes, Integer escalaMax) {
        static PerguntaDTO from(Pergunta p) {
            return new PerguntaDTO(p.getId(), p.getEnunciado(), p.getTipo(),
                    p.isObrigatoria(), p.getOpcoes(), p.getEscalaMax());
        }
    }

    public record QuestionarioDTO(String id, String eventoId, String titulo, boolean congelado,
                                  String politicaIdentificacao, List<PerguntaDTO> perguntas) {
        static QuestionarioDTO from(Questionario q) {
            return new QuestionarioDTO(q.getId(), q.getEventoId(), q.getTitulo(), q.isCongelado(),
                    q.getPoliticaIdentificacao(),
                    q.getPerguntas().stream().map(PerguntaDTO::from).toList());
        }
    }

    public record AvaliacaoDTO(String id, String questionarioId, String inscricaoId,
                               String dataSubmissao, List<RespostaDTO> respostas) {
        static AvaliacaoDTO from(Avaliacao a) {
            return new AvaliacaoDTO(a.getId(), a.getQuestionarioId(), a.getInscricaoId(),
                    a.getDataSubmissao().toString(),
                    a.getRespostas().stream().map(r -> new RespostaDTO(r.getPerguntaId(), r.getValor())).toList());
        }
    }

    public record RespostaDTO(String perguntaId, String valor) {
    }
}
