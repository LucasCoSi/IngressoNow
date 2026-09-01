package br.com.evento.adapters.in.web.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StandardError(
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public StandardError(Instant timestamp, Integer status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }

    public record FieldError(String field, String message) {}
}
