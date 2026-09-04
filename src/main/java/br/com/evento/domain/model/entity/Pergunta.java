package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * RF-24/RF-25 — pergunta de um questionário configurável.
 * Cada tipo (texto, escolha única, escala numérica) carrega sua própria
 * configuração e sabe validar a resposta correspondente (ROO-05:
 * validação polimórfica por tipo, sem if/switch espalhado nos use cases).
 */
public class Pergunta {
    private final String id;
    private final String enunciado;
    private final TipoPergunta tipo;
    private final boolean obrigatoria;
    /** RF-25: opções para ESCOLHA_SIMPLES (ex.: ["Ruim","Regular","Bom","Ótimo"]). */
    private final List<String> opcoes;
    /** RF-25: valor máximo da escala para ESCALA_NUMERICA (mínimo sempre 1). */
    private final Integer escalaMax;

    public Pergunta(String id, String enunciado, TipoPergunta tipo,
                    boolean obrigatoria, List<String> opcoes, Integer escalaMax) {
        if (id == null || id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }
        if (enunciado == null || enunciado.trim().isEmpty()) {
            throw new DomainException("O enunciado da pergunta é obrigatório.");
        }
        if (tipo == null) {
            throw new DomainException("O tipo da pergunta é obrigatório.");
        }
        if (tipo == TipoPergunta.ESCOLHA_SIMPLES && (opcoes == null || opcoes.size() < 2)) {
            throw new DomainException("Pergunta de escolha única exige ao menos 2 opções.");
        }
        if (tipo == TipoPergunta.ESCALA_NUMERICA && (escalaMax == null || escalaMax < 2)) {
            throw new DomainException("Pergunta de escala numérica exige escala máxima >= 2.");
        }
        this.enunciado = enunciado;
        this.tipo = tipo;
        this.obrigatoria = obrigatoria;
        this.opcoes = opcoes == null ? List.of() : List.copyOf(opcoes);
        this.escalaMax = escalaMax;
    }

    public Pergunta(String enunciado, TipoPergunta tipo) {
        this(null, enunciado, tipo, true, null, null);
    }

    public Pergunta(String id, String enunciado, TipoPergunta tipo) {
        this(id, enunciado, tipo, true, null, null);
    }

    public String getId() { return id; }
    public String getEnunciado() { return enunciado; }
    public TipoPergunta getTipo() { return tipo; }
    public boolean isObrigatoria() { return obrigatoria; }
    public List<String> getOpcoes() { return Collections.unmodifiableList(opcoes); }
    public Integer getEscalaMax() { return escalaMax; }

    /**
     * Valida um valor bruto de resposta contra esta pergunta.
     * Lança DomainException com mensagem compreensível (RNF-07).
     */
    public void validarResposta(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            if (obrigatoria) {
                throw new DomainException("A pergunta '" + enunciado + "' é obrigatória.");
            }
            return;
        }
        switch (tipo) {
            case TEXTO_LIVRE -> {
                // Qualquer texto não-vazio é válido.
            }
            case ESCOLHA_SIMPLES -> {
                if (!opcoes.contains(valor.trim())) {
                    throw new DomainException("A pergunta '" + enunciado
                            + "' exige uma das opções: " + String.join(", ", opcoes) + ".");
                }
            }
            case ESCALA_NUMERICA -> {
                int numero;
                try {
                    numero = Integer.parseInt(valor.trim());
                } catch (NumberFormatException e) {
                    throw new DomainException("A pergunta '" + enunciado
                            + "' exige um número de 1 a " + escalaMax + ".");
                }
                if (numero < 1 || numero > escalaMax) {
                    throw new DomainException("A pergunta '" + enunciado
                            + "' exige um número de 1 a " + escalaMax + ".");
                }
            }
        }
    }

}
