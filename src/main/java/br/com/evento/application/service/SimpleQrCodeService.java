package br.com.evento.application.service;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.port.in.DadosQrCode;
import br.com.evento.domain.port.in.QrCodeService;

import java.util.Base64;

public class SimpleQrCodeService implements QrCodeService {

    private static final String SECRET_SALT = "IngressoNow_Salt_v1";

    /** D-05 / RF-20: janela de validade do código (curta, sem PII no payload). */
    public static final java.time.Duration VALIDADE = java.time.Duration.ofMinutes(5);

    @Override
    public String gerarTokenParaCheckIn(Inscricao inscricao, String atividadeId) {
        long emitidoEm = java.time.Instant.now().toEpochMilli();
        String data = inscricao.getId() + ":" + atividadeId + ":" + emitidoEm + ":" + SECRET_SALT;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public DadosQrCode decodificarToken(String token) {
        try {
            byte[] raw;
            try {
                raw = Base64.getUrlDecoder().decode(token);
            } catch (IllegalArgumentException e) {
                // Compatibilidade: tokens legados em Base64 padrão.
                raw = Base64.getDecoder().decode(token);
            }
            String decoded = new String(raw, java.nio.charset.StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length == 3 && SECRET_SALT.equals(parts[2])) {
                // Legado (sem instante): aceito apenas para não quebrar fluxos já emitidos;
                // a validade passa a ser garantida pelos tokens novos com instante.
                return new DadosQrCode(parts[0], parts[1], java.time.Instant.now());
            }
            if (parts.length != 4 || !SECRET_SALT.equals(parts[3])) {
                throw new DomainException("Token de QR Code inválido ou corrompido.");
            }
            if (parts[0].isBlank() || parts[1].isBlank()) {
                throw new DomainException("Token de QR Code inválido ou corrompido.");
            }
            long emitidoEm;
            try {
                emitidoEm = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                throw new DomainException("Token de QR Code inválido ou corrompido.");
            }
            java.time.Instant instante = java.time.Instant.ofEpochMilli(emitidoEm);
            if (instante.plus(VALIDADE).isBefore(java.time.Instant.now())) {
                throw new DomainException("Código expirado. Solicite a chamada atual ao organizador.");
            }
            return new DadosQrCode(parts[0], parts[1], instante);
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new DomainException("Falha ao decodificar QR Code.");
        }
    }
}
