package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
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

/**
 * RF-13/RN-06: vagas por atividade.
 */
@ExtendWith(MockitoExtension.class)
class SelecionarAtividadeUseCaseTest {

    @Mock
    private InscricaoRepository inscricaoRepository;

    @Mock
    private EventoRepository eventoRepository;

    @InjectMocks
    private SelecionarAtividadeUseCase useCase;

    private Evento evento;
    private Atividade atividade;
    private Inscricao inscricao;

    @BeforeEach
    void setUp() {
        Organizador org = new Organizador("org-1", "Maria",
                new Email("maria@teste.com"), new SenhaHash("hash"));
        evento = new Evento("evt-1", "Evento Java", "Descricao", org);
        atividade = new Atividade("at-1", "Workshop",
                new Periodo(LocalDateTime.of(2026, 10, 10, 10, 0),
                        LocalDateTime.of(2026, 10, 10, 12, 0)),
                "Lab 1", "WORKSHOP", "Backend");
        atividade.configurarLimiteDeVagas(2);
        evento.adicionarAtividade(atividade);
        evento.configurarInscricao(false, true);
        evento.publicar();
        inscricao = new Inscricao("insc-1", "part-1", "evt-1", LocalDateTime.now());
    }

    @Test
    void deveSelecionarQuandoHaVaga() {
        when(inscricaoRepository.buscarPorId("insc-1")).thenReturn(Optional.of(inscricao));
        when(eventoRepository.buscarPorId("evt-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.contarPorAtividade("evt-1", "at-1")).thenReturn(1);
        when(inscricaoRepository.salvar(any(Inscricao.class))).thenAnswer(inv -> inv.getArgument(0));

        Inscricao salva = useCase.executar("insc-1", "at-1");

        assertEquals(1, salva.getAtividadesSelecionadas().size());
        verify(inscricaoRepository).salvar(any(Inscricao.class));
    }

    @Test
    void deveBloquearQuandoAtividadeLotada() {
        when(inscricaoRepository.buscarPorId("insc-1")).thenReturn(Optional.of(inscricao));
        when(eventoRepository.buscarPorId("evt-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.contarPorAtividade("evt-1", "at-1")).thenReturn(2);

        DomainException ex = assertThrows(DomainException.class,
                () -> useCase.executar("insc-1", "at-1"));
        assertTrue(ex.getMessage().contains("vagas"));
        verify(inscricaoRepository, never()).salvar(any(Inscricao.class));
    }

    @Test
    void deveIgnorarLimiteQuandoRegraPorAtividadeDesligada() {
        evento = new Evento("evt-1", "Evento Java", "Descricao",
                new Organizador("org-1", "Maria",
                        new Email("maria@teste.com"), new SenhaHash("hash")));
        Atividade semRegra = new Atividade("at-1", "Workshop",
                new Periodo(LocalDateTime.of(2026, 10, 10, 10, 0),
                        LocalDateTime.of(2026, 10, 10, 12, 0)),
                "Lab 1");
        semRegra.configurarLimiteDeVagas(1);
        evento.adicionarAtividade(semRegra);
        evento.publicar();

        when(inscricaoRepository.buscarPorId("insc-1")).thenReturn(Optional.of(inscricao));
        when(eventoRepository.buscarPorId("evt-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.salvar(any(Inscricao.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> useCase.executar("insc-1", "at-1"));
        verify(inscricaoRepository, never()).contarPorAtividade(any(), any());
    }

    @Test
    void deveIgnorarLimiteQuandoAtividadeSemLimite() {
        atividade.configurarLimiteDeVagas(null);

        when(inscricaoRepository.buscarPorId("insc-1")).thenReturn(Optional.of(inscricao));
        when(eventoRepository.buscarPorId("evt-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.salvar(any(Inscricao.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> useCase.executar("insc-1", "at-1"));
        verify(inscricaoRepository, never()).contarPorAtividade(any(), any());
    }
}
