package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.vo.Periodo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AvaliacaoTest {

    @Test
    void naoDevePermitirAvaliacaoSemPresencaValida() {
        Inscricao inscricao = new Inscricao("part-1", "evt-1");
        Atividade atividade = new Atividade("a1", "Palestra",
                new Periodo(LocalDateTime.now(), LocalDateTime.now().plusHours(1)), "Sala");
        inscricao.selecionarAtividade(atividade);
        // Sem check-in

        Questionario questionario = new Questionario("evt-1");

        DomainException ex = assertThrows(DomainException.class, () -> {
            new Avaliacao(questionario, inscricao, List.of());
        });
        assertTrue(ex.getMessage().contains("não possui presenças mínimas"));
    }

    @Test
    void devePermitirAvaliacaoComPresencaValida() {
        Inscricao inscricao = new Inscricao("part-1", "evt-1");
        Atividade atividade = new Atividade("a1", "Palestra",
                new Periodo(LocalDateTime.now(), LocalDateTime.now().plusHours(1)), "Sala");
        inscricao.selecionarAtividade(atividade);
        
        // Simula check-in
        inscricao.registrarCheckIn(atividade, LocalDateTime.now());

        Questionario questionario = new Questionario("evt-1");
        questionario.adicionarPergunta(new Pergunta("Gostou?", TipoPergunta.ESCOLHA_SIMPLES));

        Avaliacao avaliacao = new Avaliacao(questionario, inscricao, List.of(new Resposta(questionario.getPerguntas().get(0).getId(), "5")));

        assertNotNull(avaliacao.getId());
        assertEquals(inscricao.getId(), avaliacao.getInscricaoId());
    }
}
