package br.com.evento.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckInRequest(
        @NotBlank(message = "O token do QR Code é obrigatório")
        String qrCodeToken
) {}
