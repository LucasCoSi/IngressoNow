package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;

import java.util.List;

/**
 * RF-25 — validação polimórfica por tipo de pergunta (ROO-05):
 * cada constante sabe validar o valor correspondente, sem switch espalhado.
 */
public enum TipoPergunta {
    TEXTO_LIVRE {
        @Override
        public void validar(String enunciado, boolean obrigatoria,
                            List<String> opcoes, Integer escalaMax, String valor) {
            // Qualquer texto não-vazio é válido (vazio tratado antes pelo chamador).
        }
    },
    ESCOLHA_SIMPLES {
        @Override
        public void validar(String enunciado, boolean obrigatoria,
                            List<String> opcoes, Integer escalaMax, String valor) {
            List<String> efetivas = opcoes == null ? List.of() : opcoes;
            if (!efetivas.contains(valor.trim())) {
                throw new DomainException("A pergunta '" + enunciado
                        + "' exige uma das opções: " + String.join(", ", efetivas) + ".");
            }
        }
    },
    /** RF-25: escala numérica (ex.: 1 a 5). */
    ESCALA_NUMERICA {
        @Override
        public void validar(String enunciado, boolean obrigatoria,
                            List<String> opcoes, Integer escalaMax, String valor) {
            int numero;
            try {
                numero = Integer.parseInt(valor.trim());
            } catch (NumberFormatException e) {
                throw new DomainException("A pergunta '" + enunciado
                        + "' exige um número de 1 a " + escalaMax + ".");
            }
            if (numero < 1 || escalaMax == null || numero > escalaMax) {
                throw new DomainException("A pergunta '" + enunciado
                        + "' exige um número de 1 a " + escalaMax + ".");
            }
        }
    };

    public abstract void validar(String enunciado, boolean obrigatoria,
                                 List<String> opcoes, Integer escalaMax, String valor);
}
