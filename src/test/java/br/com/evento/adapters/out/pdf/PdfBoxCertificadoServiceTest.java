package br.com.evento.adapters.out.pdf;

import br.com.evento.domain.port.out.DadosCertificado;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RF-33 (D) — PDF binário real com dados verificáveis.
 */
class PdfBoxCertificadoServiceTest {

    @Test
    void deveGerarPdfComNomeECodigoVerificavel() throws Exception {
        PdfBoxCertificadoService svc = new PdfBoxCertificadoService();
        byte[] pdf = svc.gerar(new DadosCertificado(
                "Certificado de Participação", "Mariana Azevedo", null,
                "Simpósio de Sistemas 2026", 20, "ING-ABC123",
                "2026-09-08T10:00"));

        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).equals("%PDF"),
                "deve ser um PDF válido");

        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdf))) {
            String texto = new PDFTextStripper().getText(doc);
            assertTrue(texto.contains("Mariana Azevedo"));
            assertTrue(texto.contains("ING-ABC123"));
            assertTrue(texto.contains("Simpósio de Sistemas 2026"));
        }
    }

    @Test
    void deveImprimirPapelDoPalestrante() throws Exception {
        PdfBoxCertificadoService svc = new PdfBoxCertificadoService();
        byte[] pdf = svc.gerar(new DadosCertificado(
                "Declaração de Participação", "Dra. Helena", "PALESTRANTE",
                "Simpósio de Sistemas 2026", 0, "ING-XYZ",
                "2026-09-08T10:00"));

        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdf))) {
            assertTrue(new PDFTextStripper().getText(doc).contains("PALESTRANTE"));
        }
    }
}
