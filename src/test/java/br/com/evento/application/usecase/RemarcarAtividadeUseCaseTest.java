package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.EventoRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RF-07 — remarcação com revalidação de conflito contra as irmãs.
 */
class RemarcarAtividadeUseCaseTest {

    private Evento eventoComDuas() {
        var org = new Organizador("o-1", "Maria", new Email("maria@email.com"),
                new SenhaHash("h"));
        Evento evento = new Evento("e-1", "Evento", "descricao do evento", org);
        evento.adicionarAtividade(new Atividade("a-1", "Abertura",
                new Periodo(LocalDateTime.of(2026, 9, 10, 9, 0),
                        LocalDateTime.of(2026, 9, 10, 11, 0)),
                "Auditório A"));
        evento.adicionarAtividade(new Atividade("a-2", "Workshop",
                new Periodo(LocalDateTime.of(2026, 9, 10, 14, 0),
                        LocalDateTime.of(2026, 9, 10, 17, 0)),
                "Auditório A"));
        return evento;
    }

    @Test
    void deveRemarcarSemConflito() {
        Evento evento = eventoComDuas();
        EventoRepository repo = mock(EventoRepository.class);
        when(repo.buscarPorId("e-1")).thenReturn(Optional.of(evento));

        new RemarcarAtividadeUseCase(repo).executar("e-1", "a-1",
                LocalDateTime.of(2026, 9, 10, 8, 0),
                LocalDateTime.of(2026, 9, 10, 9, 0),
                "Auditório A");

        assertEquals(LocalDateTime.of(2026, 9, 10, 8, 0),
                evento.getAtividades().get(0).getPeriodo().getInicio());
        verify(repo).salvar(evento);
    }

    @Test
    void deveBloquearRemarcacaoComConflito() {
        Evento evento = eventoComDuas();
        EventoRepository repo = mock(EventoRepository.class);
        when(repo.buscarPorId("e-1")).thenReturn(Optional.of(evento));

        assertThrows(DomainException.class, () -> new RemarcarAtividadeUseCase(repo)
                .executar("e-1", "a-1",
                        LocalDateTime.of(2026, 9, 10, 15, 0),
                        LocalDateTime.of(2026, 9, 10, 16, 0),
                        "Auditório A"));

        // Período original preservado (valida antes de mutar)
        assertEquals(LocalDateTime.of(2026, 9, 10, 9, 0),
                evento.getAtividades().get(0).getPeriodo().getInicio());
    }

    @Test
    void devePermitirMesmoHorarioEmLocalDiferente() {
        Evento evento = eventoComDuas();
        EventoRepository repo = mock(EventoRepository.class);
        when(repo.buscarPorId("e-1")).thenReturn(Optional.of(evento));

        assertDoesNotThrow(() -> new RemarcarAtividadeUseCase(repo).executar("e-1", "a-1",
                LocalDateTime.of(2026, 9, 10, 15, 0),
                LocalDateTime.of(2026, 9, 10, 16, 0),
                "Sala B"));
    }
}
