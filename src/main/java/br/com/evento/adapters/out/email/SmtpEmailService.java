package br.com.evento.adapters.out.email;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.port.out.EmailMsg;
import br.com.evento.domain.port.out.EmailService;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import java.util.Properties;

/**
 * RF-34 (D) — adapter SMTP (Jakarta Mail) com o PDF em anexo.
 * Sem `smtp.host` configurado, rejeita com mensagem clara (422 no
 * handler); o download do certificado segue disponível.
 */
public class SmtpEmailService implements EmailService {

    private final String host;
    private final int port;
    private final String user;
    private final String pass;
    private final String from;

    public SmtpEmailService(String host, int port, String user, String pass, String from) {
        this.host = host == null ? "" : host.trim();
        this.port = port;
        this.user = user == null ? "" : user;
        this.pass = pass == null ? "" : pass;
        this.from = (from == null || from.isBlank()) ? this.user : from;
    }

    @Override
    public void enviar(EmailMsg msg) {
        if (host.isBlank()) {
            throw new DomainException(
                    "Envio de e-mail não configurado (defina SMTP_HOST/PORT/USER/PASS). "
                    + "O certificado permanece disponível para download.");
        }
        if (msg.para() == null || msg.para().isBlank()) {
            throw new DomainException("O e-mail de destino é obrigatório.");
        }
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(port));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });
            MimeMessage mime = new MimeMessage(session);
            mime.setFrom(new InternetAddress(from));
            mime.setRecipient(Message.RecipientType.TO, new InternetAddress(msg.para().trim()));
            mime.setSubject(msg.assunto());
            MimeBodyPart texto = new MimeBodyPart();
            texto.setText(msg.corpo() == null ? "" : msg.corpo(), "utf-8");
            MimeBodyPart anexo = new MimeBodyPart();
            anexo.setDataHandler(new jakarta.activation.DataHandler(
                    new ByteArrayDataSource(msg.anexoPdf(), "application/pdf")));
            anexo.setFileName(msg.anexoNome());
            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(texto);
            multipart.addBodyPart(anexo);
            mime.setContent(multipart);
            Transport.send(mime);
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Falha no envio do e-mail (o certificado permanece disponível para download): "
                    + e.getMessage(), e);
        }
    }
}
