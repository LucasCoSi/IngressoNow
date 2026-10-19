package br.com.evento.domain.model.entity;

import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdministradorTest {

    @Test
    void deveCriarAdministradorValido() {
        Administrador admin = new Administrador("Maria", new Email("maria@teste.com"),
                new SenhaHash("1234"));

        assertNotNull(admin.getId());
        assertEquals("Maria", admin.getNome());
        assertEquals("maria@teste.com", admin.getEmail().getEndereco());
    }
}
