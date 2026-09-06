package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.CheckIn;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RF-22 — lançamento manual com autoria (RN-11/RN-12).
 */
class RealizarCheckInManualUseCaseTest {

    private Organizador organizador() {
        return new Organizador("org-1", "Maria", new Email("maria@email.com"),
                new SenhaHash("h"));
    }

    private Evento evento() {
        Evento evento = new Evento("e-1", "Evento", "descricao do evento", organizador());
        evento.adicionarAtividade(new Atividade("a-1", "Palestra",
                new Periodo(LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1)),
                "Sala"));
        return evento;
    }

    private Inscricao inscricaoSelecionada() {
        Inscricao inscricao = new Inscricao("i-1", "part-1", "e-1", LocalDateTime.now());
        inscricao.selecionarAtividade(new Atividade("a-1", "Palestra",
                new Periodo(LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1)),
                "Sala"));
        return inscricao;
    }

    @Test
    void deveRegistrarManualComResponsavel() {
        Inscricao inscricao = inscricaoSelecionada();
        InscricaoRepository inscricoes = mock(InscricaoRepository.class);
        EventoRepository eventos = mock(EventoRepository.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        when(usuarios.buscarPorId("org-1")).thenReturn(Optional.of(organizador()));
        when(inscricoes.buscarPorId("i-1")).thenReturn(Optional.of(inscricao));
        when(eventos.buscarPorId("e-1")).thenReturn(Optional.of(evento()));

        new RealizarCheckInManualUseCase(inscricoes, eventos, usuarios)
                .executar("i-1", "a-1", "org-1");

        assertEquals(1, inscricao.getTodosCheckIns().size());
        CheckIn checkIn = inscricao.getTodosCheckIns().get(0);
        assertEquals(CheckIn.Origem.MANUAL, checkIn.getOrigem());
        assertEquals("org-1", checkIn.getResponsavelId());
        verify(inscricoes).salvar(inscricao);
    }

    @Test
    void deveRejeitarSemResponsavel() {
        assertThrows(DomainException.class, () -> new RealizarCheckInManualUseCase(
                mock(InscricaoRepository.class), mock(EventoRepository.class),
                mock(UsuarioRepository.class)).executar("i-1", "a-1", " "));
    }

    @Test
    void deveRejeitarResponsavelInexistente() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        when(usuarios.buscarPorId("x")).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> new RealizarCheckInManualUseCase(
                mock(InscricaoRepository.class), mock(EventoRepository.class),
                usuarios).executar("i-1", "a-1", "x"));
    }
}
