package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Participante;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
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
class InscreverParticipanteUseCaseTest {

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private InscricaoRepository inscricaoRepository;

    @InjectMocks
    private InscreverParticipanteUseCase useCase;

    private Participante participante;
    private Evento evento;

    @BeforeEach
    void setUp() {
        participante = new Participante("part-1", "João", new Email("joao@teste.com"), new SenhaHash("123"));
        Organizador org = new Organizador("org-1", "Maria", new Email("maria@teste.com"), new SenhaHash("hash"));
        evento = new Evento("evt-1", "Evento Java", "Desc", org);
    }

    @Test
    void deveInscreverComSucesso() {
        when(usuarioRepository.buscarPorId("part-1")).thenReturn(Optional.of(participante));
        when(eventoRepository.buscarPorId("evt-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.existePorParticipanteEEvento("part-1", "evt-1")).thenReturn(false);
        when(inscricaoRepository.salvar(any(Inscricao.class))).thenAnswer(inv -> inv.getArgument(0));

        Inscricao inscricao = useCase.executar("part-1", "evt-1");
        
        assertNotNull(inscricao);
        assertEquals("part-1", inscricao.getParticipanteId());
        assertEquals("evt-1", inscricao.getEventoId());
        verify(inscricaoRepository).salvar(any(Inscricao.class));
    }

    @Test
    void naoDeveInscreverSeJaInscrito() {
        when(usuarioRepository.buscarPorId("part-1")).thenReturn(Optional.of(participante));
        when(eventoRepository.buscarPorId("evt-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.existePorParticipanteEEvento("part-1", "evt-1")).thenReturn(true);

        DomainException ex = assertThrows(DomainException.class, () -> useCase.executar("part-1", "evt-1"));
        assertTrue(ex.getMessage().contains("já inscrito"));
    }

    @Test
    void naoDeveInscreverSeSemVagas() {
        evento.configurarLimiteDeVagas(10);
        when(usuarioRepository.buscarPorId("part-1")).thenReturn(Optional.of(participante));
        when(eventoRepository.buscarPorId("evt-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.existePorParticipanteEEvento("part-1", "evt-1")).thenReturn(false);
        when(inscricaoRepository.contarPorEvento("evt-1")).thenReturn(10); // Vagas esgotadas

        DomainException ex = assertThrows(DomainException.class, () -> useCase.executar("part-1", "evt-1"));
        assertTrue(ex.getMessage().contains("Não há vagas disponíveis"));
    }
}
