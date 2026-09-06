package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * RF-24 — questionário configurável de um evento (título + perguntas).
 * RN-17: após receber respostas, a estrutura é congelada — mudanças
 * exigiriam nova versão, nunca edição silenciosa do histórico.
 */
public class Questionario {
    private final String id;
    private final String eventoId;
    private String titulo;
    private final List<Pergunta> perguntas;
    private boolean congelado;
    /**
     * RN-15: política de identificação das respostas, informada ao
     * participante antes do envio ("IDENTIFICADO" ou "ANONIMO").
     */
    private String politicaIdentificacao = "IDENTIFICADO";

    public Questionario(String id, String eventoId, String titulo) {
        if (id == null || id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }
        if (eventoId == null || eventoId.trim().isEmpty()) {
            throw new DomainException("O evento do questionário é obrigatório.");
        }
        this.eventoId = eventoId;
        this.titulo = titulo == null ? "" : titulo;
        this.perguntas = new ArrayList<>();
        this.congelado = false;
    }

    public Questionario(String eventoId) {
        this(null, eventoId, "");
    }

    public Questionario(String id, String eventoId) {
        this(id, eventoId, "");
    }

    public String getId() { return id; }
    public String getEventoId() { return eventoId; }
    public String getTitulo() { return titulo; }
    public boolean isCongelado() { return congelado; }
    public String getPoliticaIdentificacao() { return politicaIdentificacao; }

    /** RN-15: define antes de receber respostas (congela junto). */
    public void definirPoliticaIdentificacao(String politica) {
        exigirEditavel();
        if (politica == null || politica.isBlank()) return;
        String p = politica.trim().toUpperCase();
        if (!p.equals("IDENTIFICADO") && !p.equals("ANONIMO")) {
            throw new DomainException(
                    "Política de identificação inválida (use IDENTIFICADO ou ANONIMO).");
        }
        this.politicaIdentificacao = p;
    }

    /** Restaura política da persistência (uso exclusivo dos adapters). */
    public void restaurarPolitica(String politica) {
        if (politica != null && !politica.isBlank()) this.politicaIdentificacao = politica;
    }

    public void definirTitulo(String titulo) {
        exigirEditavel();
        this.titulo = titulo == null ? "" : titulo;
    }

    public void adicionarPergunta(Pergunta pergunta) {
        exigirEditavel();
        if (pergunta == null) {
            throw new DomainException("A pergunta não pode ser nula.");
        }
        this.perguntas.add(pergunta);
    }

    public List<Pergunta> getPerguntas() {
        return Collections.unmodifiableList(perguntas);
    }

    public Pergunta buscarPergunta(String perguntaId) {
        return perguntas.stream()
                .filter(p -> p.getId().equals(perguntaId))
                .findFirst()
                .orElseThrow(() -> new DomainException("Pergunta não pertence a este questionário."));
    }

    /** Congela a estrutura após a primeira resposta (RN-17). */
    public void congelar() {
        this.congelado = true;
    }

    private void exigirEditavel() {
        if (congelado) {
            throw new DomainException("Questionário com respostas não pode ser alterado.");
        }
    }
}
