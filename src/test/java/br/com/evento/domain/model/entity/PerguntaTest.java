package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RF-25 / ROO-05: validação polimórfica por TipoPergunta (sem switch em Pergunta).
 */
class PerguntaTest {

    @Test
    void deveValidarEscalaDentroDoLimite() {
        Pergunta p = new Pergunta("p1", "Nota", TipoPergunta.ESCALA_NUMERICA, true, null, 5);
        assertDoesNotThrow(() -> p.validarResposta("4"));
        assertThrows(DomainException.class, () -> p.validarResposta("6"));
        assertThrows(DomainException.class, () -> p.validarResposta("abc"));
    }

    @Test
    void deveValidarEscolhaUnica() {
        Pergunta p = new Pergunta("p2", "Gostou?", TipoPergunta.ESCOLHA_SIMPLES,
                true, List.of("Sim", "Não"), null);
        assertDoesNotThrow(() -> p.validarResposta("Sim"));
        assertThrows(DomainException.class, () -> p.validarResposta("Talvez"));
    }

    @Test
    void deveAceitarTextoLivreEObrigatoriedade() {
        Pergunta obrigatoria = new Pergunta("p3", "Comentário", TipoPergunta.TEXTO_LIVRE);
        assertThrows(DomainException.class, () -> obrigatoria.validarResposta("  "));

        Pergunta opcional = new Pergunta("p4", "Comentário", TipoPergunta.TEXTO_LIVRE,
                false, null, null);
        assertDoesNotThrow(() -> opcional.validarResposta("  "));
        assertDoesNotThrow(() -> opcional.validarResposta("qualquer texto"));
    }
}
