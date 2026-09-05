package br.com.evento.adapters.in.http;

import br.com.evento.domain.exception.DomainException;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Respostas HTTP + mapeamento de exceções (substitui GlobalExceptionHandler):
 * DomainException→422, IllegalArgumentException→400, "não encontrad*"→404, demais→500.
 */
public final class HttpErrors {

    private HttpErrors() {
    }

    public static void enviar(HttpExchange ex, int status, Object corpo, String contentType) throws IOException {
        byte[] bytes = Json.escrever(corpo).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (var out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static void json(HttpExchange ex, int status, Object corpo) throws IOException {
        enviar(ex, status, corpo, "application/json");
    }

    public static void texto(HttpExchange ex, int status, String corpo, String contentType) throws IOException {
        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (var out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static void semConteudo(HttpExchange ex) throws IOException {
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }

    public static void tratar(HttpExchange ex, Exception e) {
        try {
            tratarInterno(ex, e);
        } catch (IOException ioe) {
            try {
                ex.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static void tratarInterno(HttpExchange ex, Exception e) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (e instanceof DomainException de) {
            if (de.getMessage() != null && de.getMessage().toLowerCase().contains("não encontrad")
                    || de.getMessage() != null && de.getMessage().toLowerCase().contains("nao encontrad")) {
                json(ex, 404, StandardError.de(404, "Not Found", de.getMessage(), path));
            } else {
                json(ex, 422, StandardError.de(422, "Unprocessable Entity", de.getMessage(), path));
            }
            return;
        }
        if (e instanceof IllegalArgumentException iae) {
            json(ex, 400, StandardError.de(400, "Bad Request", iae.getMessage(), path));
            return;
        }
        json(ex, 500, StandardError.de(500, "Internal Server Error",
                "Ocorreu um erro interno no servidor. Tente novamente mais tarde.", path));
    }

    public static String corpo(HttpExchange ex) throws IOException {
        try (var in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
