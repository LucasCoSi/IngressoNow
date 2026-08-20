package br.com.evento.domain.model.vo;

import br.com.evento.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PeriodoTest {

    @Test
    void deveCriarPeriodoValido() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 10, 10, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 10, 10, 12, 0);
        Periodo periodo = new Periodo(inicio, fim);
        
        assertEquals(inicio, periodo.getInicio());
        assertEquals(fim, periodo.getFim());
    }

    @Test
    void naoDeveCriarPeriodoComFimAntesDoInicio() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 10, 12, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 10, 10, 10, 0);
        
        assertThrows(DomainException.class, () -> new Periodo(inicio, fim));
    }

    @Test
    void deveDetectarConflitoDePeriodos() {
        Periodo p1 = new Periodo(
                LocalDateTime.of(2026, 10, 10, 10, 0),
                LocalDateTime.of(2026, 10, 10, 12, 0)
        );

        Periodo p2 = new Periodo(
                LocalDateTime.of(2026, 10, 10, 11, 0),
                LocalDateTime.of(2026, 10, 10, 13, 0)
        );

        assertTrue(p1.conflitaCom(p2));
        assertTrue(p2.conflitaCom(p1));
    }

    @Test
    void naoDeveDetectarConflitoSeNaoHouverSobreposicao() {
        Periodo p1 = new Periodo(
                LocalDateTime.of(2026, 10, 10, 10, 0),
                LocalDateTime.of(2026, 10, 10, 12, 0)
        );

        Periodo p2 = new Periodo(
                LocalDateTime.of(2026, 10, 10, 12, 0),
                LocalDateTime.of(2026, 10, 10, 14, 0)
        );

        assertFalse(p1.conflitaCom(p2));
    }
}
