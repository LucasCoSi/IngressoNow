package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrganizadorTest {

    @Test
    void deveCriarOrganizadorValido() {
        Organizador org = new Organizador("João", new Email("joao@teste.com"), new SenhaHash("1234"));
        
        assertNotNull(org.getId());
        assertEquals("João", org.getNome());
        assertEquals("joao@teste.com", org.getEmail().getEndereco());
    }

    @Test
    void naoDeveCriarSemNomeEmailOuSenha() {
        Email email = new Email("teste@teste.com");
        SenhaHash senha = new SenhaHash("123");

        assertThrows(DomainException.class, () -> new Organizador("", email, senha));
        assertThrows(DomainException.class, () -> new Organizador("João", null, senha));
        assertThrows(DomainException.class, () -> new Organizador("João", email, null));
    }
}
