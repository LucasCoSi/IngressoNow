package br.com.evento.application.usecase;

import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.StatusEvento;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.port.out.EventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListarEventosPublicadosUseCaseTest {

    @Mock
    private EventoRepository eventoRepository;

    @InjectMocks
    private ListarEventosPublicadosUseCase useCase;

    private Organizador org;

    @BeforeEach
    void setUp() {
        org = new Organizador("org-1", "Maria", new Email("maria@teste.com"), new SenhaHash("hash"));
    }

    @Test
    void deveRetornarEventosPublicadosEComInscricoes() {
        Evento publicado = criarEventoPublicado("Evento Publicado");
        Evento comInscricoes = criarEventoComInscricoes("Evento com Inscrições");

        when(eventoRepository.buscarPorStatus(StatusEvento.PUBLICADO)).thenReturn(List.of(publicado));
        when(eventoRepository.buscarPorStatus(StatusEvento.INSCRICOES_ABERTAS)).thenReturn(List.of(comInscricoes));

        List<Evento> resultado = useCase.executar();

        assertEquals(2, resultado.size());
        verify(eventoRepository).buscarPorStatus(StatusEvento.PUBLICADO);
        verify(eventoRepository).buscarPorStatus(StatusEvento.INSCRICOES_ABERTAS);
    }

    @Test
    void deveRetornarListaVaziaSeNenhumEventoPublicado() {
        when(eventoRepository.buscarPorStatus(StatusEvento.PUBLICADO)).thenReturn(List.of());
        when(eventoRepository.buscarPorStatus(StatusEvento.INSCRICOES_ABERTAS)).thenReturn(List.of());

        List<Evento> resultado = useCase.executar();

        assertTrue(resultado.isEmpty());
    }

    private Evento criarEventoPublicado(String titulo) {
        Evento evento = new Evento(titulo, "desc", org);
        Periodo p = new Periodo(LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 12, 0));
        evento.adicionarAtividade(new Atividade("Atividade", p, "Sala"));
        evento.publicar();
        return evento;
    }

    private Evento criarEventoComInscricoes(String titulo) {
        Evento evento = criarEventoPublicado(titulo);
        evento.abrirInscricoes();
        return evento;
    }
}
