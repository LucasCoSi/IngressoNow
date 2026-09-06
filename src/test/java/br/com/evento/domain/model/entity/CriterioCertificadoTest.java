package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.model.vo.SenhaHash;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RF-32 (D) / RN-16 — critério de elegibilidade calculável.
 */
class CriterioCertificadoTest {

    private Evento evento() {
        var org = new Organizador("o-1", "Maria", new Email("maria@email.com"),
                new SenhaHash("h"));
        Evento evento = new Evento("e-1", "Evento", "descricao do evento", org);
        evento.adicionarAtividade(new Atividade("a-1", "Abertura",
                new Periodo(LocalDateTime.now().minusHours(4), LocalDateTime.now().minusHours(3)),
                "Sala A"));
        evento.adicionarAtividade(new Atividade("a-2", "Palestra",
                new Periodo(LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1)),
                "Sala B"));
        return evento;
    }

    private Inscricao inscricaoCom(String... ids) {
        Evento evento = evento();
        Inscricao inscricao = new Inscricao("i-1", "part-1", "e-1", LocalDateTime.now());
        for (String id : ids) {
            Atividade a = evento.getAtividades().stream()
                    .filter(x -> x.getId().equals(id)).findFirst().orElseThrow();
            inscricao.selecionarAtividade(a);
            inscricao.registrarCheckIn(a, LocalDateTime.now());
        }
        return inscricao;
    }

    @Test
    void deveAprovarComPercentualMinimo() {
        CriterioCertificado c = new CriterioCertificado("e-1");
        c.definirPercentualMinimo(50);

        assertTrue(c.verificar(inscricaoCom("a-1", "a-2"), evento()).isEmpty());
    }

    @Test
    void deveRejeitarAbaixoDoMinimo() {
        CriterioCertificado c = new CriterioCertificado("e-1");
        c.definirPercentualMinimo(100);

        Optional<String> motivo = c.verificar(inscricaoCom("a-1"), evento());
        assertTrue(motivo.isPresent());
        assertTrue(motivo.get().contains("50%"));
    }

    @Test
    void deveExigirAtividadeObrigatoria() {
        CriterioCertificado c = new CriterioCertificado("e-1");
        c.exigirAtividade("a-2");

        // Check-in só na a-1: falta a obrigatória
        Optional<String> motivo = c.verificar(inscricaoCom("a-1", "a-2"), evento());
        // a-2 tem check-in aqui, então deve passar; remove e testa de novo
        assertTrue(motivo.isEmpty());

        Inscricao soA1 = inscricaoCom("a-1");
        Optional<String> motivo2 = c.verificar(soA1, evento());
        assertTrue(motivo2.isPresent());
        assertTrue(motivo2.get().contains("a-2"));
    }

    @Test
    void deveRejeitarPercentualInvalido() {
        CriterioCertificado c = new CriterioCertificado("e-1");
        assertThrows(DomainException.class, () -> c.definirPercentualMinimo(101));
        assertThrows(DomainException.class, () -> c.definirPercentualMinimo(-1));
    }
}
