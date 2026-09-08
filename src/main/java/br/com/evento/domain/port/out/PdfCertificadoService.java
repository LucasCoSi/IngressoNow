package br.com.evento.domain.port.out;

/**
 * RF-33 (D) — porta de saída para geração do PDF do certificado.
 * Implementada por adaptador (PDFBox); o domínio nunca importa a lib.
 */
public interface PdfCertificadoService {
    /** Gera os bytes do PDF (application/pdf). */
    byte[] gerar(DadosCertificado dados);
}
