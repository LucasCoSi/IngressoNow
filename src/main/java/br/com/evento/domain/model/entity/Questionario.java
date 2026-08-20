package br.com.evento.domain.model.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Questionario {
    private final String id;
    private final String eventoId;
    private final List<Pergunta> perguntas;

    public Questionario(String id, String eventoId) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.eventoId = eventoId;
        this.perguntas = new ArrayList<>();
    }

    public Questionario(String eventoId) {
        this(null, eventoId);
    }

    public String getId() { return id; }
    public String getEventoId() { return eventoId; }

    public void adicionarPergunta(Pergunta pergunta) {
        this.perguntas.add(pergunta);
    }

    public List<Pergunta> getPerguntas() {
        return new ArrayList<>(perguntas);
    }
}
