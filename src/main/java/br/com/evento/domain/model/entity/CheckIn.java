package br.com.evento.domain.model.entity;

import java.time.LocalDateTime;

public class CheckIn {
    private final String atividadeId;
    private final LocalDateTime dataHora;

    public CheckIn(String atividadeId, LocalDateTime dataHora) {
        this.atividadeId = atividadeId;
        this.dataHora = dataHora;
    }

    public String getAtividadeId() {
        return atividadeId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
