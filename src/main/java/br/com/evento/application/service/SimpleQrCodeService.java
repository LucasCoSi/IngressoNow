package br.com.evento.application.service;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.port.in.DadosQrCode;
import br.com.evento.domain.port.in.QrCodeService;

import java.util.Base64;

public class SimpleQrCodeService implements QrCodeService {

    private static final String SECRET_SALT = "IngressoNow_Salt_v1";

    @Override
    public String gerarTokenParaCheckIn(Inscricao inscricao, String atividadeId) {
        String data = inscricao.getId() + ":" + atividadeId + ":" + SECRET_SALT;
        return Base64.getEncoder().encodeToString(data.getBytes());
    }

    @Override
    public DadosQrCode decodificarToken(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token));
            String[] parts = decoded.split(":");
            if (parts.length != 3 || !SECRET_SALT.equals(parts[2])) {
                throw new DomainException("Token de QR Code inválido ou corrompido.");
            }
            return new DadosQrCode(parts[0], parts[1]);
        } catch (Exception e) {
            throw new DomainException("Falha ao decodificar QR Code.");
        }
    }
}
