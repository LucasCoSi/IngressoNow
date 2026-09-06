package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Participante;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.PasswordHasher;
import br.com.evento.domain.port.out.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RF-03 — consulta/atualização de dados básicos (nome, senha).
 */
class EditarUsuarioUseCaseTest {

    private Usuario participante() {
        return new Participante("u-1", "Lucas", new Email("lucas@email.com"),
                new SenhaHash("hash-antigo"));
    }

    private EditarUsuarioUseCase useCase(UsuarioRepository repo, PasswordHasher hasher) {
        return new EditarUsuarioUseCase(repo, hasher);
    }

    @Test
    void deveAtualizarNome() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        when(repo.buscarPorId("u-1")).thenReturn(Optional.of(participante()));
        when(repo.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        Usuario atualizado = useCase(repo, mock(PasswordHasher.class))
                .executar("u-1", "Lucas Silva", null);

        assertEquals("Lucas Silva", atualizado.getNome());
        assertEquals("hash-antigo", atualizado.getSenha().getHash());
    }

    @Test
    void deveAtualizarSenhaComHash() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        PasswordHasher hasher = mock(PasswordHasher.class);
        when(repo.buscarPorId("u-1")).thenReturn(Optional.of(participante()));
        when(hasher.hash("nova123")).thenReturn("hash-novo");
        when(repo.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        Usuario atualizado = useCase(repo, hasher).executar("u-1", null, "nova123");

        assertEquals("hash-novo", atualizado.getSenha().getHash());
        verify(hasher).hash("nova123");
    }

    @Test
    void deveRejeitarSenhaCurta() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        when(repo.buscarPorId("u-1")).thenReturn(Optional.of(participante()));

        assertThrows(DomainException.class,
                () -> useCase(repo, mock(PasswordHasher.class)).executar("u-1", null, "123"));
    }

    @Test
    void deveRejeitarUsuarioInexistente() {
        UsuarioRepository repo = mock(UsuarioRepository.class);
        when(repo.buscarPorId("x")).thenReturn(Optional.empty());

        assertThrows(DomainException.class,
                () -> useCase(repo, mock(PasswordHasher.class)).executar("x", "Nome", null));
    }
}
