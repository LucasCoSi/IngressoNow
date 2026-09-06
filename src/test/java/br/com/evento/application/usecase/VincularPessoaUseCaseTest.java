package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Pessoa;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.PessoaRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RF-08 — vínculo de pessoa com papel a uma atividade.
 */
class VincularPessoaUseCaseTest {

    private Evento eventoComAtividade() {
        var org = new Organizador("o-1", "Maria", new Email("maria@email.com"),
                new SenhaHash("h"));
        Evento evento = new Evento("e-1", "Evento", "descricao do evento", org);
        evento.adicionarAtividade(new Atividade("a-1", "Palestra",
                new Periodo(LocalDateTime.of(2026, 9, 10, 9, 0),
                        LocalDateTime.of(2026, 9, 10, 11, 0)),
                "Auditório A"));
        return evento;
    }

    @Test
    void deveVincularComPapel() {
        EventoRepository eventos = mock(EventoRepository.class);
        PessoaRepository pessoas = mock(PessoaRepository.class);
        when(eventos.buscarPorId("e-1")).thenReturn(Optional.of(eventoComAtividade()));
        when(pessoas.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        Pessoa p = new VincularPessoaUseCase(eventos, pessoas)
                .executar("e-1", "a-1", "Dra. Helena", "PALESTRANTE");

        assertEquals("Dra. Helena", p.getNome());
        assertEquals("PALESTRANTE", p.getPapel());
        assertEquals("a-1", p.getAtividadeId());
    }

    @Test
    void deveRejeitarAtividadeForaDoEvento() {
        EventoRepository eventos = mock(EventoRepository.class);
        when(eventos.buscarPorId("e-1")).thenReturn(Optional.of(eventoComAtividade()));

        assertThrows(DomainException.class, () -> new VincularPessoaUseCase(eventos,
                mock(PessoaRepository.class)).executar("e-1", "outra", "Nome", "PAPEL"));
    }

    @Test
    void deveRejeitarPapelVazio() {
        EventoRepository eventos = mock(EventoRepository.class);
        when(eventos.buscarPorId("e-1")).thenReturn(Optional.of(eventoComAtividade()));

        assertThrows(DomainException.class, () -> new VincularPessoaUseCase(eventos,
                mock(PessoaRepository.class)).executar("e-1", "a-1", "Nome", " "));
    }
}
