package br.com.evento.adapters.in.web.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CriarAtividadeRequest(
        @NotBlank(message = "O título da atividade é obrigatório")
        @Size(min = 3, max = 200, message = "O título deve ter entre 3 e 200 caracteres")
        String titulo,

        @NotBlank(message = "A descrição da atividade é obrigatória")
        @Size(min = 10, max = 1000, message = "A descrição deve ter entre 10 e 1000 caracteres")
        String descricao,

        @NotNull(message = "A data de início é obrigatória")
        @FutureOrPresent(message = "A data de início deve ser no presente ou futuro")
        LocalDateTime inicio,

        @NotNull(message = "A data de término é obrigatória")
        @Future(message = "A data de término deve ser no futuro")
        LocalDateTime fim,

        @NotBlank(message = "O local da atividade é obrigatório")
        @Size(min = 3, max = 200, message = "O local deve ter entre 3 e 200 caracteres")
        String local,

        @NotBlank(message = "O ID do evento é obrigatório")
        String eventoId,

        @Positive(message = "O número de vagas deve ser um valor positivo")
        Integer maxVagas
) {}
