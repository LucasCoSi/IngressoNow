package br.com.evento.domain.port.out;

/**
 * RF-34 (D) — porta de saída para envio de e-mail.
 * Sem SMTP configurado, o adaptador rejeita com mensagem clara
 * e o download do certificado permanece disponível.
 */
public interface EmailService {
    /** Envia a mensagem; lança exceção com motivo em caso de falha. */
    void enviar(EmailMsg msg);
}
