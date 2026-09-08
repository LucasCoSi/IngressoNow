package br.com.evento.application.usecase;

import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.EventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListarEventosOrganizadorUseCaseTest {

    @Mock
    private EventoRepository eventoRepository;

    @InjectMocks
    private ListarEventosOrganizadorUseCase useCase;

    private Organizador org;

    @BeforeEach
    void setUp() {
        org = new Organizador("org-1", "Maria", new Email("maria@teste.com"), new SenhaHash("hash"));
    }

    @Test
    void deveRetornarTodosOsEventosDoOrganizador() {
        // Inclui RASCUNHO: o GET público não o retorna, o de gestão sim.
        Evento rascunho = new Evento("Rascunho", "descricao longa aqui", org);
        Evento outro = new Evento("Outro", "outra descricao longa", org);

        when(eventoRepository.buscarPorOrganizador("org-1")).thenReturn(List.of(rascunho, outro));

        List<Evento> resultado = useCase.executar("org-1");

        assertEquals(2, resultado.size());
        verify(eventoRepository).buscarPorOrganizador("org-1");
    }

    @Test
    void deveRejeitarIdEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> useCase.executar(null));
        assertThrows(IllegalArgumentException.class, () -> useCase.executar("  "));
        verifyNoInteractions(eventoRepository);
    }
}
