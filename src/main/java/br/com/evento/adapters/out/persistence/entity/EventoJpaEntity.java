package br.com.evento.adapters.out.persistence.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eventos")
public class EventoJpaEntity {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false)
    private String titulo;

    private String descricao;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id", nullable = false)
    private OrganizadorJpaEntity organizador;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AtividadeJpaEntity> atividades = new ArrayList<>();

    protected EventoJpaEntity() {
        // Construtor padrão exigido pelo JPA
    }

    public EventoJpaEntity(String id, String titulo, String descricao, String status, OrganizadorJpaEntity organizador) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.status = status;
        this.organizador = organizador;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OrganizadorJpaEntity getOrganizador() { return organizador; }
    public void setOrganizador(OrganizadorJpaEntity organizador) { this.organizador = organizador; }

    public List<AtividadeJpaEntity> getAtividades() { return atividades; }
    public void setAtividades(List<AtividadeJpaEntity> atividades) { this.atividades = atividades; }
}
