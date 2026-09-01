package br.com.evento.adapters.in.web.dto;

import jakarta.validation.constraints.*;

public record CriarEventoRequest(
        @NotBlank(message = "O título do evento é obrigatório")
        @Size(min = 3, max = 200, message = "O título deve ter entre 3 e 200 caracteres")
        String titulo,

        @NotBlank(message = "A descrição do evento é obrigatória")
        @Size(min = 10, max = 2000, message = "A descrição deve ter entre 10 e 2000 caracteres")
        String descricao,

        @NotBlank(message = "O ID do organizador é obrigatório")
        String organizadorId,

        @Positive(message = "O número de vagas deve ser um valor positivo")
        Integer maxVagas
) {}
