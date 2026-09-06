package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;

import java.time.LocalDateTime;

/**
 * RF-22 / RN-11 / RN-12 — registro de frequência com rastreabilidade:
 * origem (QR_CODE ou MANUAL) e responsável (quem lançou o check-in manual).
 */
public class CheckIn {
    public enum Origem { QR_CODE, MANUAL }

    private final String atividadeId;
    private final LocalDateTime dataHora;
    private final Origem origem;
    /** Id do usuário que lançou o check-in manual (null quando via QR). */
    private final String responsavelId;

    public CheckIn(String atividadeId, LocalDateTime dataHora) {
        this(atividadeId, dataHora, Origem.QR_CODE, null);
    }

    public CheckIn(String atividadeId, LocalDateTime dataHora, Origem origem, String responsavelId) {
        if (atividadeId == null || atividadeId.isBlank()) {
            throw new DomainException("A atividade do check-in é obrigatória.");
        }
        if (dataHora == null) {
            throw new DomainException("A data/hora do check-in é obrigatória.");
        }
        if (origem == null) {
            throw new DomainException("A origem do check-in é obrigatória.");
        }
        if (origem == Origem.MANUAL && (responsavelId == null || responsavelId.isBlank())) {
            throw new DomainException("Check-in manual exige o responsável pelo lançamento.");
        }
        this.atividadeId = atividadeId;
        this.dataHora = dataHora;
        this.origem = origem;
        this.responsavelId = responsavelId;
    }

    public String getAtividadeId() {
        return atividadeId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public Origem getOrigem() {
        return origem;
    }

    public String getResponsavelId() {
        return responsavelId;
    }
}
