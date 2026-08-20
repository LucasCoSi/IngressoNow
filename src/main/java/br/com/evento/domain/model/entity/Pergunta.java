package br.com.evento.domain.model.entity;

import java.util.UUID;

public class Pergunta {
    private final String id;
    private final String enunciado;
    private final TipoPergunta tipo;

    public Pergunta(String id, String enunciado, TipoPergunta tipo) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.enunciado = enunciado;
        this.tipo = tipo;
    }

    public Pergunta(String enunciado, TipoPergunta tipo) {
        this(null, enunciado, tipo);
    }

    public String getId() { return id; }
    public String getEnunciado() { return enunciado; }
    public TipoPergunta getTipo() { return tipo; }
}
