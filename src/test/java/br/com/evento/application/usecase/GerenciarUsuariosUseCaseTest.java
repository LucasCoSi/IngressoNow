package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Participante;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Regras de remoção de usuário (§3): sem auto-remoção, sem remover quem
 * organiza eventos e sem remover participante com inscrições (FK).
 */
@ExtendWith(MockitoExtension.class)
class GerenciarUsuariosUseCaseTest {

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private EventoRepository eventos;

    @Mock
    private InscricaoRepository inscricoes;

    @InjectMocks
    private GerenciarUsuariosUseCase useCase;

    private Participante participante(String id) {
        return new Participante(id, "Alvo", new Email("alvo@email.com"), new SenhaHash("hash"));
    }

    @Test
    void naoRemoveAPropriaConta() {
        assertThrows(DomainException.class, () -> useCase.remover("u-1", "u-1"));
        verifyNoInteractions(usuarios, inscricoes);
    }

    @Test
    void naoRemoveParticipanteComInscricoes() {
        when(usuarios.buscarPorId("u-1")).thenReturn(Optional.of(participante("u-1")));
        when(inscricoes.buscarPorParticipante("u-1"))
                .thenReturn(List.of(new br.com.evento.domain.model.entity.Inscricao("p-1", "e-1")));

        DomainException erro = assertThrows(DomainException.class,
                () -> useCase.remover("admin", "u-1"));
        assertTrue(erro.getMessage().toLowerCase().contains("inscri"));
        verify(usuarios, never()).remover(any());
    }

    @Test
    void removeParticipanteSemInscricoes() {
        when(usuarios.buscarPorId("u-1")).thenReturn(Optional.of(participante("u-1")));
        when(inscricoes.buscarPorParticipante("u-1")).thenReturn(List.of());

        useCase.remover("admin", "u-1");

        verify(usuarios).remover("u-1");
    }

    @Test
    void naoRemoveOrganizadorComEventos() {
        Usuario org = new Organizador("o-1", "Org", new Email("org@email.com"), new SenhaHash("hash"));
        when(usuarios.buscarPorId("o-1")).thenReturn(Optional.of(org));
        when(eventos.buscarPorOrganizador("o-1"))
                .thenReturn(List.of(new br.com.evento.domain.model.entity.Evento(
                        "Evento do Org", "Descrição do evento de teste", org)));

        DomainException erro = assertThrows(DomainException.class,
                () -> useCase.remover("admin", "o-1"));
        assertTrue(erro.getMessage().toLowerCase().contains("evento"));
        verify(usuarios, never()).remover(any());
    }
}
