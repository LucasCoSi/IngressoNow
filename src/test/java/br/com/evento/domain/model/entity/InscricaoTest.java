package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.vo.Periodo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class InscricaoTest {

    @Test
    void deveCriarInscricaoValida() {
        Inscricao inscricao = new Inscricao("part-1", "evt-1");
        assertNotNull(inscricao.getId());
        assertEquals("part-1", inscricao.getParticipanteId());
        assertEquals("evt-1", inscricao.getEventoId());
        assertNotNull(inscricao.getDataInscricao());
        assertTrue(inscricao.getAtividadesSelecionadas().isEmpty());
    }

    @Test
    void deveSelecionarAtividadeComSucesso() {
        Inscricao inscricao = new Inscricao("part-1", "evt-1");
        Atividade atividade = new Atividade("a1", "Palestra",
                new Periodo(LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 12, 0)),
                "Sala 1");
        
        inscricao.selecionarAtividade(atividade);
        assertEquals(1, inscricao.getAtividadesSelecionadas().size());
    }

    @Test
    void naoDeveSelecionarAtividadeDuplicada() {
        Inscricao inscricao = new Inscricao("part-1", "evt-1");
        Atividade atividade = new Atividade("a1", "Palestra",
                new Periodo(LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 12, 0)),
                "Sala 1");
        
        inscricao.selecionarAtividade(atividade);
        
        DomainException ex = assertThrows(DomainException.class, () -> inscricao.selecionarAtividade(atividade));
        assertTrue(ex.getMessage().contains("já está selecionada"));
    }

    @Test
    void naoDeveSelecionarAtividadeComConflitoDeHorario() {
        Inscricao inscricao = new Inscricao("part-1", "evt-1");
        Atividade a1 = new Atividade("a1", "Palestra 1",
                new Periodo(LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 12, 0)),
                "Sala 1");
        Atividade a2 = new Atividade("a2", "Palestra 2",
                new Periodo(LocalDateTime.of(2026, 1, 1, 11, 0), LocalDateTime.of(2026, 1, 1, 13, 0)),
                "Sala 2");
        
        inscricao.selecionarAtividade(a1);
        
        DomainException ex = assertThrows(DomainException.class, () -> inscricao.selecionarAtividade(a2));
        assertTrue(ex.getMessage().contains("conflita com a atividade"));
    }

    @Test
    void deveGerarAgendaOrdenadaCronologicamente() {
        Inscricao inscricao = new Inscricao("part-1", "evt-1");
        Atividade a1 = new Atividade("a1", "Palestra 1",
                new Periodo(LocalDateTime.of(2026, 1, 1, 14, 0), LocalDateTime.of(2026, 1, 1, 16, 0)),
                "Sala 1");
        Atividade a2 = new Atividade("a2", "Palestra 2",
                new Periodo(LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 12, 0)),
                "Sala 2");
        
        inscricao.selecionarAtividade(a1);
        inscricao.selecionarAtividade(a2);
        
        AgendaDoParticipante agenda = inscricao.gerarAgenda();
        assertEquals(2, agenda.totalAtividades());
        assertEquals("a2", agenda.getAtividades().get(0).getId()); // a2 starts earlier
        assertEquals("a1", agenda.getAtividades().get(1).getId());
    }
}
