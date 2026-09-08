package br.com.evento.domain.port.out;

/**
 * RF-33 (D) — dados para impressão do certificado em PDF.
 * Montado pelo use case a partir do agregado (sem dependência de PDF no domínio).
 */
public record DadosCertificado(String tituloDocumento, String nome, String papelDeclarado,
                               String eventoTitulo, int cargaHoraria, String codigo,
                               String emitidoEm) {
}
