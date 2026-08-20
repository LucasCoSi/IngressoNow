package br.com.evento.domain.model.evento;

import java.util.UUID;

/**
 * Entidade que representa uma Atividade dentro de um Evento.
 * Seu ciclo de vida é gerenciado pelo Evento (Aggregate Root).
 */
public class Atividade {
    
    private final UUID id;
    private final Evento evento; // Relacionamento forte com a raiz
    private String nome;
    private Horario horario;
    private int vagas;

    // Construtor package-private: apenas Evento pode criar (protegendo a relação)
    Atividade(Evento evento, String nome, Horario horario, int vagas) {
        if (evento == null) throw new IllegalArgumentException("Atividade precisa pertencer a um evento.");
        if (nome == null || nome.trim().isEmpty()) throw new IllegalArgumentException("Nome da atividade é obrigatório.");
        if (horario == null) throw new IllegalArgumentException("Horário é obrigatório.");
        if (vagas < 0) throw new IllegalArgumentException("A quantidade de vagas não pode ser negativa.");
        
        this.id = UUID.randomUUID();
        this.evento = evento;
        this.nome = nome;
        this.horario = horario;
        this.vagas = vagas;
    }

    public void alterarHorario(Horario novoHorario) {
        if (this.evento.getEstado() != EstadoEvento.RASCUNHO) {
             throw new IllegalStateException("Não é possível alterar o horário após o evento ser publicado.");
        }
        this.horario = novoHorario;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public Horario getHorario() { return horario; }
    public int getVagas() { return vagas; }
    public Evento getEvento() { return evento; }
}
