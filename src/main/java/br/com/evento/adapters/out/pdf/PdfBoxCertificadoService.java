package br.com.evento.adapters.out.pdf;

import br.com.evento.domain.port.out.DadosCertificado;
import br.com.evento.domain.port.out.PdfCertificadoService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * RF-33 (D) — adapter PDFBox: imprime o certificado em 1 página A4
 * (título, nome/papel, evento, carga horária, código verificável).
 */
public class PdfBoxCertificadoService implements PdfCertificadoService {

    @Override
    public byte[] gerar(DadosCertificado dados) {
        try (PDDocument doc = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);
            try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                float cx = PDRectangle.A4.getWidth() / 2;
                centralizado(cs, cx, 740, dados.tituloDocumento(), PDType1Font.HELVETICA_BOLD, 26);
                centralizado(cs, cx, 700, "IngressoNow — Plataforma de Gestão de Eventos",
                        PDType1Font.HELVETICA, 11);
                centralizado(cs, cx, 640, dados.nome(), PDType1Font.HELVETICA_BOLD, 20);
                if (dados.papelDeclarado() != null && !dados.papelDeclarado().isBlank()) {
                    centralizado(cs, cx, 612, "Papel exercido: " + dados.papelDeclarado(),
                            PDType1Font.HELVETICA, 12);
                }
                centralizado(cs, cx, 570, dados.eventoTitulo(), PDType1Font.HELVETICA, 14);
                centralizado(cs, cx, 540, "Carga horária: " + dados.cargaHoraria() + "h",
                        PDType1Font.HELVETICA, 12);
                centralizado(cs, cx, 480, "Código verificável: " + dados.codigo(),
                        PDType1Font.HELVETICA_BOLD, 12);
                centralizado(cs, cx, 460, "Emitido em: " + dados.emitidoEm(),
                        PDType1Font.HELVETICA, 10);
                centralizado(cs, cx, 120, "Valide em /api/certificados?codigo=" + dados.codigo(),
                        PDType1Font.HELVETICA_OBLIQUE, 9);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gerar PDF do certificado", e);
        }
    }

    private static void centralizado(PDPageContentStream cs, float cx, float y,
                                     String texto, PDType1Font fonte, int tamanho) throws IOException {
        cs.beginText();
        cs.setFont(fonte, tamanho);
        float largura = fonte.getStringWidth(texto == null ? "" : texto) / 1000 * tamanho;
        cs.newLineAtOffset(cx - largura / 2, y);
        cs.showText(texto == null ? "" : texto);
        cs.endText();
    }
}
