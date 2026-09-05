package br.com.evento.adapters.in.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson compartilhado (substitui a serialização automática do Spring).
 */
public final class Json {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Json() {
    }

    public static String escrever(Object valor) {
        try {
            return MAPPER.writeValueAsString(valor);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar JSON", e);
        }
    }

    public static <T> T ler(String corpo, Class<T> tipo) {
        try {
            return MAPPER.readValue(corpo == null || corpo.isBlank() ? "{}" : corpo, tipo);
        } catch (Exception e) {
            throw new IllegalArgumentException("Corpo JSON inválido.");
        }
    }
}
