package br.com.evento.adapters.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "atividades")
public class AtividadeJpaEntity {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Column(nullable = false)
    private String local;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoJpaEntity evento;

    protected AtividadeJpaEntity() {
        // Construtor padrão exigido pelo JPA
    }

    public AtividadeJpaEntity(String id, String titulo, LocalDateTime inicio, LocalDateTime fim, String local) {
        this.id = id;
        this.titulo = titulo;
        this.inicio = inicio;
        this.fim = fim;
        this.local = local;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public LocalDateTime getInicio() { return inicio; }
    public void setInicio(LocalDateTime inicio) { this.inicio = inicio; }

    public LocalDateTime getFim() { return fim; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }

    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }

    public EventoJpaEntity getEvento() { return evento; }
    public void setEvento(EventoJpaEntity evento) { this.evento = evento; }
}
