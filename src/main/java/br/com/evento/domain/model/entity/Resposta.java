package br.com.evento.domain.model.entity;

public class Resposta {
    private final String perguntaId;
    private final String valor;

    public Resposta(String perguntaId, String valor) {
        this.perguntaId = perguntaId;
        this.valor = valor;
    }

    public String getPerguntaId() { return perguntaId; }
    public String getValor() { return valor; }
}
