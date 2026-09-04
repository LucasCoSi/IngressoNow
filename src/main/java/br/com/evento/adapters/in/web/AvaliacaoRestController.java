package br.com.evento.adapters.in.web;

import br.com.evento.application.usecase.ConsolidarAvaliacoesUseCase;
import br.com.evento.application.usecase.CriarQuestionarioUseCase;
import br.com.evento.application.usecase.SubmeterAvaliacaoUseCase;
import br.com.evento.domain.model.entity.Avaliacao;
import br.com.evento.domain.model.entity.Pergunta;
import br.com.evento.domain.model.entity.Questionario;
import br.com.evento.domain.model.entity.Resposta;
import br.com.evento.domain.model.entity.TipoPergunta;
import br.com.evento.domain.port.out.QuestionarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AvaliacaoRestController {

    private final CriarQuestionarioUseCase criarQuestionarioUseCase;
    private final SubmeterAvaliacaoUseCase submeterAvaliacaoUseCase;
    private final ConsolidarAvaliacoesUseCase consolidarAvaliacoesUseCase;
    private final QuestionarioRepository questionarioRepository;

    public AvaliacaoRestController(CriarQuestionarioUseCase criarQuestionarioUseCase,
                                   SubmeterAvaliacaoUseCase submeterAvaliacaoUseCase,
                                   ConsolidarAvaliacoesUseCase consolidarAvaliacoesUseCase,
                                   QuestionarioRepository questionarioRepository) {
        this.criarQuestionarioUseCase = criarQuestionarioUseCase;
        this.submeterAvaliacaoUseCase = submeterAvaliacaoUseCase;
        this.consolidarAvaliacoesUseCase = consolidarAvaliacoesUseCase;
        this.questionarioRepository = questionarioRepository;
    }

    @PostMapping("/eventos/{eventoId}/questionarios")
    public ResponseEntity<QuestionarioDTO> criarQuestionario(@PathVariable String eventoId,
                                                             @Valid @RequestBody CriarQuestionarioRequest request) {
        List<Pergunta> perguntas = request.perguntas().stream()
                .map(p -> new Pergunta(null, p.enunciado(), p.tipo(),
                        p.obrigatoria() != null ? p.obrigatoria() : true,
                        p.opcoes(), p.escalaMax()))
                .collect(Collectors.toList());
        Questionario questionario = criarQuestionarioUseCase.executar(eventoId, request.titulo(), perguntas);
        return ResponseEntity.status(HttpStatus.CREATED).body(QuestionarioDTO.from(questionario));
    }

    @GetMapping("/eventos/{eventoId}/questionarios")
    public ResponseEntity<List<QuestionarioDTO>> listarPorEvento(@PathVariable String eventoId) {
        List<QuestionarioDTO> dtos = questionarioRepository.buscarPorEvento(eventoId).stream()
                .map(QuestionarioDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/avaliacoes")
    public ResponseEntity<AvaliacaoDTO> submeter(@Valid @RequestBody SubmeterAvaliacaoRequest request) {
        List<Resposta> respostas = request.respostas().stream()
                .map(r -> new Resposta(r.perguntaId(), r.valor()))
                .collect(Collectors.toList());
        Avaliacao avaliacao = submeterAvaliacaoUseCase.executar(
                request.questionarioId(), request.inscricaoId(), respostas);
        return ResponseEntity.status(HttpStatus.CREATED).body(AvaliacaoDTO.from(avaliacao));
    }

    @GetMapping("/questionarios/{questionarioId}/resultados")
    public ResponseEntity<ConsolidarAvaliacoesUseCase.ResultadoConsolidado> consolidar(
            @PathVariable String questionarioId) {
        return ResponseEntity.ok(consolidarAvaliacoesUseCase.executar(questionarioId));
    }

    public record CriarQuestionarioRequest(
            String titulo,
            @NotEmpty(message = "O questionário exige ao menos uma pergunta") List<PerguntaRequest> perguntas) {}

    public record PerguntaRequest(
            @NotBlank(message = "O enunciado da pergunta é obrigatório") String enunciado,
            TipoPergunta tipo,
            Boolean obrigatoria,
            List<String> opcoes,
            Integer escalaMax) {}

    public record SubmeterAvaliacaoRequest(
            @NotBlank(message = "O questionário é obrigatório") String questionarioId,
            @NotBlank(message = "A inscrição é obrigatória") String inscricaoId,
            @NotEmpty(message = "A avaliação exige ao menos uma resposta") List<RespostaRequest> respostas) {}

    public record RespostaRequest(
            @NotBlank(message = "A pergunta da resposta é obrigatória") String perguntaId,
            String valor) {}

    public record PerguntaDTO(String id, String enunciado, TipoPergunta tipo,
                              boolean obrigatoria, List<String> opcoes, Integer escalaMax) {
        static PerguntaDTO from(Pergunta p) {
            return new PerguntaDTO(p.getId(), p.getEnunciado(), p.getTipo(),
                    p.isObrigatoria(), p.getOpcoes(), p.getEscalaMax());
        }
    }

    public record QuestionarioDTO(String id, String eventoId, String titulo,
                                  boolean congelado, List<PerguntaDTO> perguntas) {
        static QuestionarioDTO from(Questionario q) {
            List<PerguntaDTO> perguntas = q.getPerguntas().stream()
                    .map(PerguntaDTO::from)
                    .collect(Collectors.toList());
            return new QuestionarioDTO(q.getId(), q.getEventoId(), q.getTitulo(),
                    q.isCongelado(), perguntas);
        }
    }

    public record AvaliacaoDTO(String id, String questionarioId, String inscricaoId,
                               String dataSubmissao, List<RespostaDTO> respostas) {
        static AvaliacaoDTO from(Avaliacao a) {
            List<RespostaDTO> respostas = a.getRespostas().stream()
                    .map(r -> new RespostaDTO(r.getPerguntaId(), r.getValor()))
                    .collect(Collectors.toList());
            return new AvaliacaoDTO(a.getId(), a.getQuestionarioId(), a.getInscricaoId(),
                    a.getDataSubmissao().toString(), respostas);
        }
    }

    public record RespostaDTO(String perguntaId, String valor) {}
}
