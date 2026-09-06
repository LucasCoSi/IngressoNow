package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RF-08 — pessoa vinculada com papel livre.
 */
class PessoaTest {

    @Test
    void deveCriarComPapelLivre() {
        Pessoa p = new Pessoa("at-1", "Dra. Helena", "PALESTRANTE");

        assertNotNull(p.getId());
        assertEquals("at-1", p.getAtividadeId());
        assertEquals("Dra. Helena", p.getNome());
        assertEquals("PALESTRANTE", p.getPapel());
    }

    @Test
    void deveRejeitarNomeVazio() {
        assertThrows(DomainException.class, () -> new Pessoa("at-1", " ", "PALESTRANTE"));
    }

    @Test
    void deveRejeitarPapelVazio() {
        assertThrows(DomainException.class, () -> new Pessoa("at-1", "Nome", null));
    }

    @Test
    void deveRejeitarSemAtividade() {
        assertThrows(DomainException.class, () -> new Pessoa(" ", "Nome", "PAPEL"));
    }
}
