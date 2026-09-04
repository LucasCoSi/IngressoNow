package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RF-15 — cancelamento de inscrição libera vaga;
 * evento encerrado bloqueia o cancelamento.
 */
class CancelarInscricaoUseCaseTest {

    private Organizador organizador() {
        return new Organizador("org-1", "Org", new Email("org@teste.com"),
                new SenhaHash("hash-valido-para-teste-1234567890"));
    }

    private Evento eventoComStatus(br.com.evento.domain.model.entity.StatusEvento status) {
        Evento evento = new Evento("evt-1", "Evento", "Descricao do evento", organizador());
        evento.adicionarAtividade(new Atividade("a1", "Atividade",
                new Periodo(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2)),
                "Sala"));
        if (status == br.com.evento.domain.model.entity.StatusEvento.PUBLICADO) evento.publicar();
        if (status == br.com.evento.domain.model.entity.StatusEvento.ENCERRADO) {
            evento.publicar();
            evento.encerrar();
        }
        return evento;
    }

    @Test
    void deveCancelarQuandoEventoPublicado() {
        Inscricao inscricao = new Inscricao("i-1", "part-1", "evt-1", LocalDateTime.now());
        InscricaoRepository inscricaoRepo = mock(InscricaoRepository.class);
        EventoRepository eventoRepo = mock(EventoRepository.class);
        when(inscricaoRepo.buscarPorId("i-1")).thenReturn(Optional.of(inscricao));
        when(eventoRepo.buscarPorId("evt-1")).thenReturn(
                Optional.of(eventoComStatus(br.com.evento.domain.model.entity.StatusEvento.PUBLICADO)));

        new CancelarInscricaoUseCase(inscricaoRepo, eventoRepo).executar("i-1");

        verify(inscricaoRepo).remover("i-1");
    }

    @Test
    void deveBloquearQuandoEventoEncerrado() {
        Inscricao inscricao = new Inscricao("i-1", "part-1", "evt-1", LocalDateTime.now());
        InscricaoRepository inscricaoRepo = mock(InscricaoRepository.class);
        EventoRepository eventoRepo = mock(EventoRepository.class);
        when(inscricaoRepo.buscarPorId("i-1")).thenReturn(Optional.of(inscricao));
        when(eventoRepo.buscarPorId("evt-1")).thenReturn(
                Optional.of(eventoComStatus(br.com.evento.domain.model.entity.StatusEvento.ENCERRADO)));

        assertThrows(DomainException.class,
                () -> new CancelarInscricaoUseCase(inscricaoRepo, eventoRepo).executar("i-1"));
        verify(inscricaoRepo, never()).remover(any());
    }

    @Test
    void deveRejeitarInscricaoEmEventoRascunho() {
        InscricaoRepository inscricaoRepo = mock(InscricaoRepository.class);
        EventoRepository eventoRepo = mock(EventoRepository.class);
        when(inscricaoRepo.buscarPorId(any())).thenReturn(Optional.empty());
        // RN-04: InscreverParticipanteUseCase exige PUBLICADO/INSCRICOES_ABERTAS
        Evento rascunho = new Evento("evt-1", "Evento", "Descricao do evento", organizador());
        when(eventoRepo.buscarPorId("evt-1")).thenReturn(Optional.of(rascunho));
        var participante = new br.com.evento.domain.model.entity.Participante("part-1", "P",
                new Email("p@teste.com"), new SenhaHash("hash-valido-para-teste-1234567890"));
        var usuarioRepo = mock(br.com.evento.domain.port.out.UsuarioRepository.class);
        when(usuarioRepo.buscarPorId("part-1")).thenReturn(Optional.of(participante));
        InscreverParticipanteUseCase inscrever =
                new InscreverParticipanteUseCase(eventoRepo, usuarioRepo, inscricaoRepo);

        assertThrows(DomainException.class, () -> inscrever.executar("part-1", "evt-1"));
    }
}
