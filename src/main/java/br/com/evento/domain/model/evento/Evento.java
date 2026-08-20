package br.com.evento.domain.model.evento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Entidade Raiz (Aggregate Root) que representa um Evento.
 * Protege regras de negócio (invariantes) sobre publicação e atividades (ROO-02).
 */
public class Evento {

    private final UUID id;
    private String titulo;
    private String descricao;
    private EstadoEvento estado;
    private final List<Atividade> atividades;

    public Evento(String titulo, String descricao) {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new IllegalArgumentException("O título do evento é obrigatório.");
        }
        this.id = UUID.randomUUID();
        this.titulo = titulo;
        this.descricao = descricao;
        this.estado = EstadoEvento.RASCUNHO;
        this.atividades = new ArrayList<>();
    }

    /**
     * Aplicação do padrão State em conceito (mudança de estado válida) e invariantes.
     */
    public void publicar() {
        if (this.estado != EstadoEvento.RASCUNHO) {
            throw new IllegalStateException("Apenas eventos em rascunho podem ser publicados.");
        }
        if (this.atividades.isEmpty()) {
            throw new IllegalStateException("O evento precisa ter pelo menos uma atividade para ser publicado.");
        }
        this.estado = EstadoEvento.PUBLICADO;
    }

    public void cancelar() {
        if (this.estado == EstadoEvento.ENCERRADO) {
            throw new IllegalStateException("Eventos encerrados não podem ser cancelados.");
        }
        this.estado = EstadoEvento.CANCELADO;
    }

    public void adicionarAtividade(String nome, Horario horario, int vagas) {
        if (this.estado != EstadoEvento.RASCUNHO) {
            throw new IllegalStateException("Só é possível adicionar atividades enquanto o evento é um rascunho.");
        }
        
        for (Atividade existente : atividades) {
            if (existente.getHorario().conflitaCom(horario)) {
                throw new IllegalArgumentException("Já existe uma atividade neste horário.");
            }
        }

        Atividade novaAtividade = new Atividade(this, nome, horario, vagas);
        this.atividades.add(novaAtividade);
    }

    // Apenas Getters (Sem setters)
    public UUID getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public EstadoEvento getEstado() { return estado; }
    
    // Retorna lista não modificável para proteger encapsulamento
    public List<Atividade> getAtividades() {
        return Collections.unmodifiableList(atividades);
    }
}
