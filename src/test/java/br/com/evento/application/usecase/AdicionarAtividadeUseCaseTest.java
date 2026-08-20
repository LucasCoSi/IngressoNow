package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.EventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdicionarAtividadeUseCaseTest {

    @Mock
    private EventoRepository eventoRepository;

    @InjectMocks
    private AdicionarAtividadeUseCase useCase;

    private Evento evento;

    @BeforeEach
    void setUp() {
        Organizador org = new Organizador("org-1", "Maria", new Email("maria@teste.com"), new SenhaHash("hash"));
        evento = new Evento("evt-1", "Conferência Java", "Descrição", org);
    }

    @Test
    void deveAdicionarAtividadeComSucesso() {
        when(eventoRepository.buscarPorId("evt-1")).thenReturn(Optional.of(evento));
        when(eventoRepository.salvar(any(Evento.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime inicio = LocalDateTime.of(2026, 10, 10, 10, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 10, 10, 12, 0);

        Evento resultado = useCase.executar("evt-1", "Palestra Spring", inicio, fim, "Auditório 1");

        assertEquals(1, resultado.getAtividades().size());
        assertEquals("Palestra Spring", resultado.getAtividades().get(0).getTitulo());
        verify(eventoRepository).salvar(any(Evento.class));
    }

    @Test
    void deveLancarExcecaoSeEventoNaoEncontrado() {
        when(eventoRepository.buscarPorId("inexistente")).thenReturn(Optional.empty());

        assertThrows(DomainException.class,
                () -> useCase.executar("inexistente", "Palestra", LocalDateTime.now(), LocalDateTime.now().plusHours(1), "Sala 1"));

        verify(eventoRepository, never()).salvar(any());
    }

    @Test
    void deveLancarExcecaoSeHouverConflitoDeHorarioELocal() {
        Periodo p1 = new Periodo(LocalDateTime.of(2026, 10, 10, 10, 0), LocalDateTime.of(2026, 10, 10, 12, 0));
        evento.adicionarAtividade(new Atividade("Palestra existente", p1, "Auditório 1"));

        when(eventoRepository.buscarPorId("evt-1")).thenReturn(Optional.of(evento));

        // Mesmo local, horário que conflita
        assertThrows(DomainException.class,
                () -> useCase.executar("evt-1", "Outra palestra",
                        LocalDateTime.of(2026, 10, 10, 11, 0),
                        LocalDateTime.of(2026, 10, 10, 13, 0),
                        "Auditório 1"));

        verify(eventoRepository, never()).salvar(any());
    }
}
