package br.com.evento.adapters.in.http;

import java.time.Instant;
import java.util.List;

/**
 * Envelope de erro — mesmo contrato do antigo GlobalExceptionHandler:
 * {timestamp, status, error, message, path, fieldErrors?}.
 * timestamp é String ISO-8601 (evita módulo jsr310 no Jackson).
 */
public record StandardError(
        String timestamp,
        Integer status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

    public static StandardError de(int status, String error, String message, String path) {
        return new StandardError(Instant.now().toString(), status, error, message, path, null);
    }

    public static StandardError de(int status, String error, String message, String path,
                                   List<FieldError> fieldErrors) {
        return new StandardError(Instant.now().toString(), status, error, message, path, fieldErrors);
    }
}
