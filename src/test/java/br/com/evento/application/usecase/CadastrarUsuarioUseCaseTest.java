package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Administrador;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Participante;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * §3/RN-18: participante é self-service; organizador e administrador exigem
 * solicitante administrador. Senha mínima única (8) validada no núcleo.
 */
@ExtendWith(MockitoExtension.class)
class CadastrarUsuarioUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private br.com.evento.domain.port.out.PasswordHasher hasher;

    @InjectMocks
    private CadastrarUsuarioUseCase useCase;

    private Usuario administrador() {
        return new Administrador("admin-1", "Admin", new Email("admin@email.com"),
                new SenhaHash("hash-admin"));
    }

    private Usuario executar(String tipo, Usuario solicitante, String senha) {
        lenient().when(usuarioRepository.existePorEmail(any())).thenReturn(false);
        lenient().when(hasher.hash(anyString())).thenReturn("hash");
        lenient().when(usuarioRepository.salvar(any())).thenAnswer(i -> i.getArgument(0));
        return useCase.executar("Nome", "a@b.com", senha, tipo, solicitante);
    }

    @Test
    void deveCriarParticipanteSemLogin() {
        assertInstanceOf(Participante.class, executar("PARTICIPANTE", null, "senha123"));
    }

    @Test
    void deveCriarOrganizadorQuandoSolicitanteEhAdmin() {
        assertInstanceOf(Organizador.class, executar("ORGANIZADOR", administrador(), "senha123"));
    }

    @Test
    void deveCriarAdministradorQuandoSolicitanteEhAdmin() {
        Usuario u = executar("ADMINISTRADOR", administrador(), "senha123");
        assertInstanceOf(Administrador.class, u);
        assertEquals("a@b.com", u.getEmail().getEndereco());
    }

    @Test
    void deveRecusarOrganizadorSemAdmin() {
        assertThrows(DomainException.class,
                () -> executar("ORGANIZADOR", null, "senha123"));
    }

    @Test
    void deveRecusarAdministradorSemAdmin() {
        assertThrows(DomainException.class,
                () -> executar("ADMINISTRADOR", null, "senha123"));
    }

    @Test
    void deveRecusarSenhaCurta() {
        assertThrows(DomainException.class,
                () -> executar("PARTICIPANTE", null, "1234567"));
    }

    @Test
    void deveAceitarSenhaComOitoCaracteres() {
        assertInstanceOf(Participante.class, executar("PARTICIPANTE", null, "12345678"));
    }

    @Test
    void deveRejeitarEmailDuplicadoETipoInvalido() {
        when(usuarioRepository.existePorEmail(any())).thenReturn(true);
        assertThrows(DomainException.class,
                () -> useCase.executar("Nome", "a@b.com", "senha123", "PARTICIPANTE", null));

        when(usuarioRepository.existePorEmail(any())).thenReturn(false);
        assertThrows(DomainException.class,
                () -> useCase.executar("Nome", "a@b.com", "senha123", "GERENTE", null));
    }
}
