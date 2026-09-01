package br.com.evento.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InscreverRequest(
        @NotBlank(message = "O ID do participante é obrigatório")
        @Size(min = 36, max = 36, message = "O ID do participante deve ser um UUID válido")
        String participanteId,

        @NotBlank(message = "O ID do evento é obrigatório")
        @Size(min = 36, max = 36, message = "O ID do evento deve ser um UUID válido")
        String eventoId
) {}
