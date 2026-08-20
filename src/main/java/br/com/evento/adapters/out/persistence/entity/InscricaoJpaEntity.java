package br.com.evento.adapters.out.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inscricoes")
public class InscricaoJpaEntity {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "participante_id", nullable = false, length = 36)
    private String participanteId;

    @Column(name = "evento_id", nullable = false, length = 36)
    private String eventoId;

    @Column(name = "data_inscricao", nullable = false)
    private LocalDateTime dataInscricao;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "inscricao_atividades",
            joinColumns = @JoinColumn(name = "inscricao_id"),
            inverseJoinColumns = @JoinColumn(name = "atividade_id")
    )
    private List<AtividadeJpaEntity> atividadesSelecionadas = new ArrayList<>();

    protected InscricaoJpaEntity() {
        // Construtor padrão exigido pelo JPA
    }

    public InscricaoJpaEntity(String id, String participanteId, String eventoId, LocalDateTime dataInscricao) {
        this.id = id;
        this.participanteId = participanteId;
        this.eventoId = eventoId;
        this.dataInscricao = dataInscricao;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParticipanteId() { return participanteId; }
    public void setParticipanteId(String participanteId) { this.participanteId = participanteId; }

    public String getEventoId() { return eventoId; }
    public void setEventoId(String eventoId) { this.eventoId = eventoId; }

    public LocalDateTime getDataInscricao() { return dataInscricao; }
    public void setDataInscricao(LocalDateTime dataInscricao) { this.dataInscricao = dataInscricao; }

    public List<AtividadeJpaEntity> getAtividadesSelecionadas() { return atividadesSelecionadas; }
    public void setAtividadesSelecionadas(List<AtividadeJpaEntity> atividadesSelecionadas) { this.atividadesSelecionadas = atividadesSelecionadas; }
}
