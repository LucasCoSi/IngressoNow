package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.StatusEvento;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CadastrarEventoUseCaseTest {

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CadastrarEventoUseCase useCase;

    private Organizador organizador;

    @BeforeEach
    void setUp() {
        organizador = new Organizador("org-1", "Maria Organizadora", new Email("maria@teste.com"), new SenhaHash("hash"));
    }

    @Test
    void deveCadastrarEventoComSucesso() {
        when(usuarioRepository.buscarPorId("org-1")).thenReturn(Optional.of(organizador));
        when(eventoRepository.salvar(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Evento evento = useCase.executar("Conferência Java", "Descrição da conferência", "org-1");

        assertNotNull(evento);
        assertEquals("Conferência Java", evento.getTitulo());
        assertEquals(StatusEvento.RASCUNHO, evento.getStatus());
        assertEquals(organizador, evento.getOrganizador());

        verify(eventoRepository).salvar(any(Evento.class));
    }

    @Test
    void deveLancarExcecaoSeOrganizadorNaoEncontrado() {
        when(usuarioRepository.buscarPorId("inexistente")).thenReturn(Optional.empty());

        DomainException ex = assertThrows(DomainException.class,
                () -> useCase.executar("Evento", "Desc", "inexistente"));

        assertTrue(ex.getMessage().contains("Organizador não encontrado"));
        verify(eventoRepository, never()).salvar(any());
    }
}
