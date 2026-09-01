package br.com.evento.adapters.in.web.dto;

public record LoginResponse(
    String token,
    String tipo,
    String usuarioId,
    String nome,
    String email
) {
    public LoginResponse(String token, String usuarioId, String nome, String email) {
        this(token, "Bearer", usuarioId, nome, email);
    }
}
