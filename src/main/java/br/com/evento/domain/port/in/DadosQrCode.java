package br.com.evento.domain.port.in;

import java.time.Instant;

/** RF-20/RF-21 — dados decodificados do QR opaco (sem PII): vínculo + instante de emissão. */
public record DadosQrCode(String inscricaoId, String atividadeId, Instant emitidoEm) {
    public DadosQrCode(String inscricaoId, String atividadeId) {
        this(inscricaoId, atividadeId, Instant.now());
    }
}
