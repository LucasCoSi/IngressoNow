package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RF-22 / RN-11 / RN-12 — rastreabilidade da frequência.
 */
class CheckInOrigemTest {

    @Test
    void deveCriarQrSemResponsavel() {
        CheckIn checkIn = new CheckIn("at-1", LocalDateTime.now());

        assertEquals(CheckIn.Origem.QR_CODE, checkIn.getOrigem());
        assertNull(checkIn.getResponsavelId());
    }

    @Test
    void deveCriarManualComResponsavel() {
        CheckIn checkIn = new CheckIn("at-1", LocalDateTime.now(),
                CheckIn.Origem.MANUAL, "org-1");

        assertEquals(CheckIn.Origem.MANUAL, checkIn.getOrigem());
        assertEquals("org-1", checkIn.getResponsavelId());
    }

    @Test
    void deveRejeitarManualSemResponsavel() {
        assertThrows(DomainException.class, () -> new CheckIn("at-1",
                LocalDateTime.now(), CheckIn.Origem.MANUAL, null));
    }

    @Test
    void deveRegistrarManualNaInscricao() {
        Atividade atividade = new Atividade("a-1", "Palestra",
                new br.com.evento.domain.model.vo.Periodo(
                        LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1)),
                "Sala");
        Inscricao inscricao = new Inscricao("i-1", "part-1", "e-1", LocalDateTime.now());
        inscricao.selecionarAtividade(atividade);

        inscricao.registrarCheckInManual(atividade, LocalDateTime.now(), "org-1");

        assertEquals(1, inscricao.getTodosCheckIns().size());
        assertEquals(CheckIn.Origem.MANUAL,
                inscricao.getTodosCheckIns().get(0).getOrigem());
    }
}
