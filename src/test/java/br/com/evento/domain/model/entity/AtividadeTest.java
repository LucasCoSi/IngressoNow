package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.vo.Periodo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AtividadeTest {

    @Test
    void deveDetectarConflitoDeAtividades() {
        Periodo p1 = new Periodo(LocalDateTime.of(2026, 10, 10, 10, 0), LocalDateTime.of(2026, 10, 10, 12, 0));
        Periodo p2 = new Periodo(LocalDateTime.of(2026, 10, 10, 11, 0), LocalDateTime.of(2026, 10, 10, 13, 0));

        Atividade a1 = new Atividade("Palestra 1", p1, "Sala 1");
        Atividade a2 = new Atividade("Palestra 2", p2, "Sala 2");

        assertTrue(a1.conflitaCom(a2));
    }

    @Test
    void naoDeveTerConflitoEmAtividadesSucessivas() {
        Periodo p1 = new Periodo(LocalDateTime.of(2026, 10, 10, 10, 0), LocalDateTime.of(2026, 10, 10, 12, 0));
        Periodo p2 = new Periodo(LocalDateTime.of(2026, 10, 10, 12, 0), LocalDateTime.of(2026, 10, 10, 14, 0));

        Atividade a1 = new Atividade("Palestra 1", p1, "Sala 1");
        Atividade a2 = new Atividade("Palestra 2", p2, "Sala 2");

        assertFalse(a1.conflitaCom(a2));
    }
}
