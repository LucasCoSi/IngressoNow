package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Avaliacao;
import br.com.evento.domain.model.entity.Pergunta;
import br.com.evento.domain.model.entity.Questionario;
import br.com.evento.domain.model.entity.Resposta;
import br.com.evento.domain.model.entity.TipoPergunta;
import br.com.evento.domain.port.out.AvaliacaoRepository;
import br.com.evento.domain.port.out.QuestionarioRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConsolidarAvaliacoesUseCase {

    private final QuestionarioRepository questionarioRepository;
    private final AvaliacaoRepository avaliacaoRepository;

    public ConsolidarAvaliacoesUseCase(QuestionarioRepository questionarioRepository,
                                       AvaliacaoRepository avaliacaoRepository) {
        this.questionarioRepository = questionarioRepository;
        this.avaliacaoRepository = avaliacaoRepository;
    }

    public ResultadoConsolidado executar(String questionarioId) {
        Questionario questionario = questionarioRepository.buscarPorId(questionarioId)
                .orElseThrow(() -> new DomainException("Questionário não encontrado."));
        List<Avaliacao> avaliacoes = avaliacaoRepository.buscarPorQuestionario(questionarioId);
        List<ConsolidadoPergunta> perguntas = new ArrayList<>();
        for (Pergunta pergunta : questionario.getPerguntas()) {
            List<String> valores = new ArrayList<>();
            for (Avaliacao avaliacao : avaliacoes) {
                for (Resposta resposta : avaliacao.getRespostas()) {
                    if (resposta.getPerguntaId().equals(pergunta.getId())) {
                        valores.add(resposta.getValor());
                    }
                }
            }
            perguntas.add(consolidar(pergunta, valores));
        }
        return new ResultadoConsolidado(questionarioId, avaliacoes.size(), perguntas);
    }

    private ConsolidadoPergunta consolidar(Pergunta pergunta, List<String> valores) {
        if (pergunta.getTipo() == TipoPergunta.ESCALA_NUMERICA) {
            double media = valores.stream()
                    .mapToInt(v -> Integer.parseInt(v.trim()))
                    .average().orElse(0.0);
            Map<String, Long> distribuicao = new LinkedHashMap<>();
            for (String valor : valores) {
                distribuicao.merge(valor.trim(), 1L, Long::sum);
            }
            return new ConsolidadoPergunta(pergunta.getId(), pergunta.getEnunciado(),
                    pergunta.getTipo(), valores.size(), media, distribuicao, List.of());
        }
        if (pergunta.getTipo() == TipoPergunta.ESCOLHA_SIMPLES) {
            Map<String, Long> distribuicao = new LinkedHashMap<>();
            for (String opcao : pergunta.getOpcoes()) {
                distribuicao.put(opcao, 0L);
            }
            for (String valor : valores) {
                distribuicao.merge(valor.trim(), 1L, Long::sum);
            }
            return new ConsolidadoPergunta(pergunta.getId(), pergunta.getEnunciado(),
                    pergunta.getTipo(), valores.size(), null, distribuicao, List.of());
        }
        return new ConsolidadoPergunta(pergunta.getId(), pergunta.getEnunciado(),
                pergunta.getTipo(), valores.size(), null, Map.of(), List.copyOf(valores));
    }

    public record ConsolidadoPergunta(String perguntaId, String enunciado, TipoPergunta tipo,
                                      int totalRespostas, Double media,
                                      Map<String, Long> distribuicao, List<String> comentarios) {}

    public record ResultadoConsolidado(String questionarioId, int totalAvaliacoes,
                                       List<ConsolidadoPergunta> perguntas) {}
}
