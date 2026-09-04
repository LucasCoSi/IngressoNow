package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;

/**
 * RF-25 — resposta a uma pergunta. A validação do valor é delegada
 * à própria {@link Pergunta} (validação polimórfica por tipo).
 */
public class Resposta {
    private final String perguntaId;
    private final String valor;

    public Resposta(String perguntaId, String valor) {
        if (perguntaId == null || perguntaId.trim().isEmpty()) {
            throw new DomainException("A pergunta da resposta é obrigatória.");
        }
        this.perguntaId = perguntaId;
        this.valor = valor;
    }

    public String getPerguntaId() { return perguntaId; }
    public String getValor() { return valor; }
}
