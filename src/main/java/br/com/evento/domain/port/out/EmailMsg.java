package br.com.evento.domain.port.out;

/**
 * RF-34 (D) — mensagem de e-mail com o PDF do certificado em anexo.
 */
public record EmailMsg(String para, String assunto, String corpo,
                       String anexoNome, byte[] anexoPdf) {
}
