package br.com.evento.domain.model.vo;

import br.com.evento.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void deveCriarEmailValido() {
        Email email = new Email("teste@exemplo.com");
        assertEquals("teste@exemplo.com", email.getEndereco());
    }

    @Test
    void deveConverterParaMinusculasERemoverEspacos() {
        Email email = new Email("  TESTE@Exemplo.COM  ");
        assertEquals("teste@exemplo.com", email.getEndereco());
    }

    @Test
    void naoDeveCriarEmailInvalido() {
        assertThrows(DomainException.class, () -> new Email("teste@"));
        assertThrows(DomainException.class, () -> new Email("teste.com"));
        assertThrows(DomainException.class, () -> new Email(""));
        assertThrows(DomainException.class, () -> new Email(null));
    }

    @Test
    void deveSerIgualSeEnderecoForIgual() {
        Email e1 = new Email("a@b.com");
        Email e2 = new Email("a@b.com");
        assertEquals(e1, e2);
    }
}
