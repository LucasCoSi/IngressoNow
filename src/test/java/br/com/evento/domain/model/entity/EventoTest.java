package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.model.vo.SenhaHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventoTest {

    private Organizador organizador;

    @BeforeEach
    void setUp() {
        organizador = new Organizador("Org Teste", new Email("org@teste.com"), new SenhaHash("hash"));
    }

    @Test
    void deveCriarEventoEmRascunho() {
        Evento evento = new Evento("Evento Java", "Descrição legal", organizador);
        
        assertEquals(StatusEvento.RASCUNHO, evento.getStatus());
        assertTrue(evento.getAtividades().isEmpty());
    }

    @Test
    void naoDevePublicarEventoSemAtividades() {
        Evento evento = new Evento("Evento Java", "Descrição legal", organizador);
        
        DomainException ex = assertThrows(DomainException.class, evento::publicar);
        assertEquals("O evento precisa ter pelo menos uma atividade para ser publicado.", ex.getMessage());
    }

    @Test
    void devePublicarEventoComAtividades() {
        Evento evento = new Evento("Evento Java", "Descrição legal", organizador);
        Periodo p = new Periodo(LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 12, 0));
        evento.adicionarAtividade(new Atividade("Abertura", p, "Auditório Principal"));
        
        evento.publicar();
        assertEquals(StatusEvento.PUBLICADO, evento.getStatus());
    }

    @Test
    void deveNaoAdicionarAtividadeEmConflitoNoMesmoLocal() {
        Evento evento = new Evento("Evento Java", "Descrição legal", organizador);
        Periodo p1 = new Periodo(LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 12, 0));
        Periodo p2 = new Periodo(LocalDateTime.of(2026, 1, 1, 11, 0), LocalDateTime.of(2026, 1, 1, 13, 0));
        
        evento.adicionarAtividade(new Atividade("Abertura", p1, "Auditório 1"));
        
        DomainException ex = assertThrows(DomainException.class, () -> 
            evento.adicionarAtividade(new Atividade("Workshop", p2, "Auditório 1"))
        );
        assertEquals("Conflito de horário e local com outra atividade do evento.", ex.getMessage());
    }

    @Test
    void deveAdicionarAtividadeEmConflitoSeLocaisDiferentes() {
        Evento evento = new Evento("Evento Java", "Descrição legal", organizador);
        Periodo p1 = new Periodo(LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 12, 0));
        Periodo p2 = new Periodo(LocalDateTime.of(2026, 1, 1, 11, 0), LocalDateTime.of(2026, 1, 1, 13, 0));
        
        evento.adicionarAtividade(new Atividade("Abertura", p1, "Auditório 1"));
        evento.adicionarAtividade(new Atividade("Workshop", p2, "Sala 2")); // locais diferentes
        
        assertEquals(2, evento.getAtividades().size());
    }

    @Test
    void deveFazerTransicaoCorretaDeStatus() {
        Evento evento = new Evento("Evento Java", "Descrição legal", organizador);
        Periodo p = new Periodo(LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 12, 0));
        evento.adicionarAtividade(new Atividade("Abertura", p, "Auditório Principal"));
        
        evento.publicar();
        evento.abrirInscricoes();
        assertEquals(StatusEvento.INSCRICOES_ABERTAS, evento.getStatus());

        evento.encerrar();
        assertEquals(StatusEvento.ENCERRADO, evento.getStatus());
    }

    @Test
    void naoDeveAbrirInscricoesSeEmRascunho() {
        Evento evento = new Evento("Evento Java", "Descrição legal", organizador);

        DomainException ex = assertThrows(DomainException.class, evento::abrirInscricoes);
        assertEquals("Inscrições só podem ser abertas para eventos publicados.", ex.getMessage());
    }

    @Test
    void deveAtualizarTituloEDescricaoSeEmRascunho() {
        Evento evento = new Evento("Titulo Antigo", "Descricao antiga", organizador);

        evento.atualizar("Titulo Novo", "Descricao nova");

        assertEquals("Titulo Novo", evento.getTitulo());
        assertEquals("Descricao nova", evento.getDescricao());
    }

    @Test
    void deveIgnorarCamposNulosOuVaziosNaAtualizacao() {
        Evento evento = new Evento("Titulo Antigo", "Descricao antiga", organizador);

        evento.atualizar(null, null);
        evento.atualizar("   ", "Descricao nova");

        assertEquals("Titulo Antigo", evento.getTitulo());
        assertEquals("Descricao nova", evento.getDescricao());
    }

    @Test
    void naoDeveAtualizarEventoPublicado() {
        Evento evento = new Evento("Evento Java", "Descrição legal", organizador);
        Periodo p = new Periodo(LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 12, 0));
        evento.adicionarAtividade(new Atividade("Abertura", p, "Auditório Principal"));
        evento.publicar();

        DomainException ex = assertThrows(DomainException.class,
                () -> evento.atualizar("Novo Titulo", "Nova descricao"));
        assertEquals("Apenas eventos em rascunho podem ser editados.", ex.getMessage());
    }
}
