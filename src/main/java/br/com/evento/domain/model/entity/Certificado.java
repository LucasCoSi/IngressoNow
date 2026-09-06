package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * RF-33/35 (D) — certificado/declaração emitido para participante
 * elegível (ou palestrante/apresentador por papel exercido).
 * Identificador verificável: código + data de emissão.
 */
public class Certificado {

    private final String id;
    private final String codigo;
    private final String eventoId;
    private final String participanteId;
    private final String participanteNome;
    /** Para RF-35: nome/papel quando é declaração de palestrante. */
    private final String papelDeclarado;
    private final int cargaHoraria;
    private final LocalDateTime emitidoEm;

    public Certificado(String id, String codigo, String eventoId, String participanteId,
                       String participanteNome, String papelDeclarado,
                       int cargaHoraria, LocalDateTime emitidoEm) {
        if (id == null || id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }
        if (codigo == null || codigo.isBlank()) {
            throw new DomainException("O código verificável é obrigatório.");
        }
        if (eventoId == null || eventoId.isBlank()) {
            throw new DomainException("O evento é obrigatório.");
        }
        if (participanteId == null || participanteId.isBlank()) {
            throw new DomainException("O participante é obrigatório.");
        }
        if (cargaHoraria < 0) {
            throw new DomainException("Carga horária inválida.");
        }
        this.codigo = codigo;
        this.eventoId = eventoId;
        this.participanteId = participanteId;
        this.participanteNome = participanteNome == null ? "" : participanteNome;
        this.papelDeclarado = papelDeclarado;
        this.cargaHoraria = cargaHoraria;
        this.emitidoEm = emitidoEm != null ? emitidoEm : LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getEventoId() { return eventoId; }
    public String getParticipanteId() { return participanteId; }
    public String getParticipanteNome() { return participanteNome; }
    public String getPapelDeclarado() { return papelDeclarado; }
    public int getCargaHoraria() { return cargaHoraria; }
    public LocalDateTime getEmitidoEm() { return emitidoEm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Certificado that = (Certificado) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
