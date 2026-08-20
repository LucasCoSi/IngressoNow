package br.com.evento.domain.model.vo;

import br.com.evento.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SenhaHashTest {

    @Test
    void deveCriarSenhaHash() {
        SenhaHash senha = new SenhaHash("hash_seguro_aqui");
        assertEquals("hash_seguro_aqui", senha.getHash());
    }

    @Test
    void naoDeveCriarSenhaHashVaziaOuNula() {
        assertThrows(DomainException.class, () -> new SenhaHash(""));
        assertThrows(DomainException.class, () -> new SenhaHash("   "));
        assertThrows(DomainException.class, () -> new SenhaHash(null));
    }
}
