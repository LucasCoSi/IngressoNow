package br.com.evento.domain.model.evento;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class EventoTest {

    @Test
    void naoDevePublicarEventoSemAtividades() {
        Evento evento = new Evento("Simpósio de TI", "Evento sobre tecnologia");
        
        IllegalStateException exception = assertThrows(IllegalStateException.class, evento::publicar);
        assertEquals("O evento precisa ter pelo menos uma atividade para ser publicado.", exception.getMessage());
    }

    @Test
    void devePublicarEventoComAtividades() {
        Evento evento = new Evento("Simpósio de TI", "Evento sobre tecnologia");
        Horario horario = new Horario(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));
        evento.adicionarAtividade("Palestra Inicial", horario, 100);
        
        assertDoesNotThrow(evento::publicar);
        assertEquals(EstadoEvento.PUBLICADO, evento.getEstado());
    }

    @Test
    void naoDeveAdicionarAtividadeEmEventoPublicado() {
        Evento evento = new Evento("Simpósio de TI", "Evento sobre tecnologia");
        Horario horario = new Horario(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));
        evento.adicionarAtividade("Palestra Inicial", horario, 100);
        evento.publicar();

        Horario novoHorario = new Horario(LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(2));
        
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            evento.adicionarAtividade("Outra Palestra", novoHorario, 50);
        });
        assertEquals("Só é possível adicionar atividades enquanto o evento é um rascunho.", exception.getMessage());
    }

    @Test
    void naoDeveAdicionarAtividadeEmConflitoDeHorario() {
        Evento evento = new Evento("Simpósio de TI", "Evento sobre tecnologia");
        
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fim = inicio.plusHours(2);
        
        Horario horario1 = new Horario(inicio, fim);
        evento.adicionarAtividade("Palestra 1", horario1, 100);

        Horario horarioConflitante = new Horario(inicio.plusHours(1), fim.plusHours(1));
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            evento.adicionarAtividade("Palestra Conflitante", horarioConflitante, 50);
        });
        
        assertEquals("Já existe uma atividade neste horário.", exception.getMessage());
    }
}
