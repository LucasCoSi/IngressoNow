package br.com.evento.domain.port.in;

import br.com.evento.domain.model.entity.Inscricao;

public interface QrCodeService {
    String gerarTokenParaCheckIn(Inscricao inscricao, String atividadeId);
    DadosQrCode decodificarToken(String token);
}
