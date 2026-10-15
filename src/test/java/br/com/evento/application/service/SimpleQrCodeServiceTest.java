package br.com.evento.application.service;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.port.in.DadosQrCode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * D-05 / RF-20 / RF-21: QR opaco com validade curta e sem PII.
 */
class SimpleQrCodeServiceTest {

    private final SimpleQrCodeService service = new SimpleQrCodeService();

    @Test
    void deveGerarTokenDecodificavelSemDadosSensiveis() {
        Inscricao inscricao = new Inscricao("insc-1", "part-1", "evt-1",
                java.time.LocalDateTime.now());
        String token = service.gerarTokenParaCheckIn(inscricao, "at-1");

        DadosQrCode dados = service.decodificarToken(token);
        assertEquals("insc-1", dados.inscricaoId());
        assertEquals("at-1", dados.atividadeId());
        assertNotNull(dados.emitidoEm());

        String bruto = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        assertFalse(bruto.toLowerCase().contains("senha"));
        assertFalse(bruto.contains("@"));
    }

    @Test
    void deveRejeitarTokenExpirado() {
        long antigo = java.time.Instant.now().minus(SimpleQrCodeService.VALIDADE).minusSeconds(60).toEpochMilli();
        String bruto = "insc-1:at-1:" + antigo + ":IngressoNow_Salt_v1";
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(bruto.getBytes(StandardCharsets.UTF_8));

        DomainException ex = assertThrows(DomainException.class, () -> service.decodificarToken(token));
        assertTrue(ex.getMessage().toLowerCase().contains("expirado"));
    }

    @Test
    void deveRejeitarTokenAdulterado() {
        assertThrows(DomainException.class, () -> service.decodificarToken("nao-e-base64-valido!!!"));
        String falso = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("a:b:c:SALT_ERRADO".getBytes(StandardCharsets.UTF_8));
        assertThrows(DomainException.class, () -> service.decodificarToken(falso));
    }
}
