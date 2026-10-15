package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.StatusPresenca;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.port.in.DadosQrCode;
import br.com.evento.domain.port.in.QrCodeService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealizarCheckInQrCodeUseCaseTest {

    @Mock
    private QrCodeService qrCodeService;
    @Mock
    private InscricaoRepository inscricaoRepository;
    @Mock
    private EventoRepository eventoRepository;

    @InjectMocks
    private RealizarCheckInQrCodeUseCase useCase;

    private Inscricao inscricao;
    private Evento evento;
    private Atividade atividade;

    @BeforeEach
    void setUp() {
        inscricao = new Inscricao("part-1", "evt-1");
        Organizador organizador = new Organizador("org-1", "Org",
                new br.com.evento.domain.model.vo.Email("org@teste.com"),
                new br.com.evento.domain.model.vo.SenhaHash("hash-valido-para-teste-1234567890"));
        evento = new Evento("evt-1", "Evento Teste", "Desc", organizador);
        
        atividade = new Atividade("a1", "Palestra 1", 
                new Periodo(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2)), 
                "Sala 1");
                
        evento.adicionarAtividade(atividade);
        inscricao.selecionarAtividade(atividade);
    }

    @Test
    void deveRealizarCheckInComSucesso() {
        String token = "token-valido";
        when(qrCodeService.decodificarToken(token)).thenReturn(new DadosQrCode(inscricao.getId(), atividade.getId()));
        when(inscricaoRepository.buscarPorId(inscricao.getId())).thenReturn(Optional.of(inscricao));
        when(eventoRepository.buscarPorId("evt-1")).thenReturn(Optional.of(evento));

        useCase.executar(token);

        assertEquals(StatusPresenca.PRESENTE, inscricao.calcularPresenca(atividade));
        verify(inscricaoRepository).salvar(inscricao);
    }

    @Test
    void naoDeveRealizarCheckInSeTokenInvalido() {
        String token = "token-invalido";
        when(qrCodeService.decodificarToken(token)).thenThrow(new DomainException("Token inválido"));

        assertThrows(DomainException.class, () -> useCase.executar(token));
        verify(inscricaoRepository, never()).salvar(any());
    }

    @Test
    void naoDeveDuplicarCheckInUnico() {
        String token = "token-valido";
        when(qrCodeService.decodificarToken(token)).thenReturn(new DadosQrCode(inscricao.getId(), atividade.getId()));
        when(inscricaoRepository.buscarPorId(inscricao.getId())).thenReturn(Optional.of(inscricao));
        when(eventoRepository.buscarPorId("evt-1")).thenReturn(Optional.of(evento));

        useCase.executar(token);

        DomainException ex = assertThrows(DomainException.class, () -> useCase.executar(token));
        assertTrue(ex.getMessage().toLowerCase().contains("já registrada"));
    }
}
