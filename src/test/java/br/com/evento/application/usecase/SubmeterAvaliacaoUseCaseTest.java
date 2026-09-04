package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Avaliacao;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Pergunta;
import br.com.evento.domain.model.entity.Questionario;
import br.com.evento.domain.model.entity.Resposta;
import br.com.evento.domain.model.entity.TipoPergunta;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.port.out.AvaliacaoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.QuestionarioRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RF-26/RF-27 — elegibilidade e anti-duplicidade na submissão.
 */
class SubmeterAvaliacaoUseCaseTest {

    private Questionario questionarioComEscala() {
        Questionario q = new Questionario("q-1", "evt-1", "Avaliação");
        q.adicionarPergunta(new Pergunta("p-esc", "Nota geral",
                TipoPergunta.ESCALA_NUMERICA, true, null, 5));
        return q;
    }

    private Inscricao inscricaoComPresenca() {
        Inscricao inscricao = new Inscricao("part-1", "evt-1");
        Atividade atividade = new Atividade("a1", "Palestra",
                new Periodo(LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1)), "Sala");
        inscricao.selecionarAtividade(atividade);
        inscricao.registrarCheckIn(atividade, LocalDateTime.now().minusHours(1));
        return inscricao;
    }

    @Test
    void deveSalvarQuandoElegivelEDadosValidos() {
        Questionario q = questionarioComEscala();
        Inscricao inscricao = inscricaoComPresenca();

        InscricaoRepository inscricaoRepo = mock(InscricaoRepository.class);
        QuestionarioRepository questionarioRepo = mock(QuestionarioRepository.class);
        AvaliacaoRepository avaliacaoRepo = mock(AvaliacaoRepository.class);
        when(questionarioRepo.buscarPorId("q-1")).thenReturn(Optional.of(q));
        when(inscricaoRepo.buscarPorId(inscricao.getId())).thenReturn(Optional.of(inscricao));
        when(avaliacaoRepo.existePorQuestionarioEInscricao("q-1", inscricao.getId())).thenReturn(false);
        when(avaliacaoRepo.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmeterAvaliacaoUseCase useCase =
                new SubmeterAvaliacaoUseCase(inscricaoRepo, questionarioRepo, avaliacaoRepo);
        Avaliacao salva = useCase.executar("q-1", inscricao.getId(),
                List.of(new Resposta("p-esc", "5")));

        assertNotNull(salva.getId());
        assertTrue(q.isCongelado());
        verify(questionarioRepo).salvar(q);
    }

    @Test
    void deveRejeitarRespostaDuplicada() {
        Questionario q = questionarioComEscala();
        Inscricao inscricao = inscricaoComPresenca();

        InscricaoRepository inscricaoRepo = mock(InscricaoRepository.class);
        QuestionarioRepository questionarioRepo = mock(QuestionarioRepository.class);
        AvaliacaoRepository avaliacaoRepo = mock(AvaliacaoRepository.class);
        when(questionarioRepo.buscarPorId("q-1")).thenReturn(Optional.of(q));
        when(inscricaoRepo.buscarPorId(inscricao.getId())).thenReturn(Optional.of(inscricao));
        when(avaliacaoRepo.existePorQuestionarioEInscricao("q-1", inscricao.getId())).thenReturn(true);

        SubmeterAvaliacaoUseCase useCase =
                new SubmeterAvaliacaoUseCase(inscricaoRepo, questionarioRepo, avaliacaoRepo);
        assertThrows(DomainException.class, () -> useCase.executar("q-1", inscricao.getId(),
                List.of(new Resposta("p-esc", "4"))));
    }

    @Test
    void deveRejeitarEscalaForaDoIntervalo() {
        Questionario q = questionarioComEscala();
        Inscricao inscricao = inscricaoComPresenca();

        InscricaoRepository inscricaoRepo = mock(InscricaoRepository.class);
        QuestionarioRepository questionarioRepo = mock(QuestionarioRepository.class);
        AvaliacaoRepository avaliacaoRepo = mock(AvaliacaoRepository.class);
        when(questionarioRepo.buscarPorId("q-1")).thenReturn(Optional.of(q));
        when(inscricaoRepo.buscarPorId(inscricao.getId())).thenReturn(Optional.of(inscricao));
        when(avaliacaoRepo.existePorQuestionarioEInscricao("q-1", inscricao.getId())).thenReturn(false);

        SubmeterAvaliacaoUseCase useCase =
                new SubmeterAvaliacaoUseCase(inscricaoRepo, questionarioRepo, avaliacaoRepo);
        DomainException ex = assertThrows(DomainException.class, () -> useCase.executar("q-1",
                inscricao.getId(), List.of(new Resposta("p-esc", "9"))));
        assertTrue(ex.getMessage().contains("1 a 5"));
    }
}
