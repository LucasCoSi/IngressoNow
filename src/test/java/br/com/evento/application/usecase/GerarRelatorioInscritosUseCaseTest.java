package br.com.evento.application.usecase;

import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Participante;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RF-29 — relatório de inscritos por evento (CSV).
 */
class GerarRelatorioInscritosUseCaseTest {

    @Test
    void deveGerarCsvComInscritos() {
        var org = new Organizador("o-1", "Maria", new Email("maria@email.com"),
                new SenhaHash("h"));
        Evento evento = new Evento("e-1", "Evento Teste", "descricao do evento", org);
        Inscricao inscricao = new Inscricao("i-1", "part-1", "e-1",
                LocalDateTime.of(2026, 9, 5, 10, 0));
        var participante = new Participante("part-1", "Lucas Silva",
                new Email("lucas@email.com"), new SenhaHash("h"));

        EventoRepository eventos = mock(EventoRepository.class);
        InscricaoRepository inscricoes = mock(InscricaoRepository.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        when(eventos.buscarPorId("e-1")).thenReturn(Optional.of(evento));
        when(inscricoes.buscarPorEvento("e-1")).thenReturn(List.of(inscricao));
        when(usuarios.buscarPorId("part-1")).thenReturn(Optional.of(participante));

        String csv = new GerarRelatorioInscritosUseCase(eventos, inscricoes, usuarios)
                .executarEmCsv("e-1");

        assertTrue(csv.startsWith("InscricaoID,ParticipanteID,ParticipanteNome,"));
        assertTrue(csv.contains("i-1,part-1,Lucas Silva,e-1,Evento Teste,"));
    }

    @Test
    void deveGerarSoCabecalhoSemInscritos() {
        var org = new Organizador("o-1", "Maria", new Email("maria@email.com"),
                new SenhaHash("h"));
        EventoRepository eventos = mock(EventoRepository.class);
        InscricaoRepository inscricoes = mock(InscricaoRepository.class);
        when(eventos.buscarPorId("e-1")).thenReturn(
                Optional.of(new Evento("e-1", "Evento", "descricao do evento", org)));
        when(inscricoes.buscarPorEvento("e-1")).thenReturn(List.of());

        String csv = new GerarRelatorioInscritosUseCase(eventos, inscricoes,
                mock(UsuarioRepository.class)).executarEmCsv("e-1");

        assertEquals("InscricaoID,ParticipanteID,ParticipanteNome,"
                + "EventoID,EventoTitulo,DataInscricao,AtividadesSelecionadas\n", csv);
    }
}
