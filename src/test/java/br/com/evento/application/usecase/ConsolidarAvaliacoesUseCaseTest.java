package br.com.evento.application.usecase;

import br.com.evento.domain.model.entity.Avaliacao;
import br.com.evento.domain.model.entity.Pergunta;
import br.com.evento.domain.model.entity.Questionario;
import br.com.evento.domain.model.entity.Resposta;
import br.com.evento.domain.model.entity.TipoPergunta;
import br.com.evento.domain.port.out.AvaliacaoRepository;
import br.com.evento.domain.port.out.QuestionarioRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RF-28 — consolidação: média/distribuição da escala, distribuição da
 * escolha única e listagem de comentários do texto livre.
 */
class ConsolidarAvaliacoesUseCaseTest {

    @Test
    void deveConsolidarOsTresTipos() {
        Questionario q = new Questionario("q-1", "evt-1", "Avaliação");
        q.adicionarPergunta(new Pergunta("p-esc", "Nota", TipoPergunta.ESCALA_NUMERICA, true, null, 5));
        q.adicionarPergunta(new Pergunta("p-opt", "Recomenda?",
                TipoPergunta.ESCOLHA_SIMPLES, true, List.of("Sim", "Não"), null));
        q.adicionarPergunta(new Pergunta("p-txt", "Comentário", TipoPergunta.TEXTO_LIVRE));

        Avaliacao a1 = new Avaliacao("a-1", "q-1", "i-1",
                List.of(new Resposta("p-esc", "5"), new Resposta("p-opt", "Sim"),
                        new Resposta("p-txt", "Ótimo!")),
                null);
        Avaliacao a2 = new Avaliacao("a-2", "q-1", "i-2",
                List.of(new Resposta("p-esc", "3"), new Resposta("p-opt", "Não"),
                        new Resposta("p-txt", "Pode melhorar")),
                null);

        QuestionarioRepository questionarioRepo = mock(QuestionarioRepository.class);
        AvaliacaoRepository avaliacaoRepo = mock(AvaliacaoRepository.class);
        when(questionarioRepo.buscarPorId("q-1")).thenReturn(Optional.of(q));
        when(avaliacaoRepo.buscarPorQuestionario("q-1")).thenReturn(List.of(a1, a2));

        ConsolidarAvaliacoesUseCase useCase =
                new ConsolidarAvaliacoesUseCase(questionarioRepo, avaliacaoRepo);
        ConsolidarAvaliacoesUseCase.ResultadoConsolidado resultado = useCase.executar("q-1");

        assertEquals(2, resultado.totalAvaliacoes());
        assertEquals(4.0, resultado.perguntas().get(0).media());
        assertEquals(1L, resultado.perguntas().get(1).distribuicao().get("Sim"));
        assertEquals(2, resultado.perguntas().get(2).comentarios().size());
    }
}
