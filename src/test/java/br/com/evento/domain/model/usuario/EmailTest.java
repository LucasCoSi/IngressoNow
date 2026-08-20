package br.com.evento.domain.model.usuario;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void deveCriarEmailValido() {
        Email email = new Email("teste@exemplo.com");
        assertEquals("teste@exemplo.com", email.getEndereco());
    }

    @Test
    void deveRejeitarEmailInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Email("email_invalido"));
        assertThrows(IllegalArgumentException.class, () -> new Email("@exemplo.com"));
        assertThrows(IllegalArgumentException.class, () -> new Email("teste@"));
        assertThrows(IllegalArgumentException.class, () -> new Email(""));
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
    }
}
