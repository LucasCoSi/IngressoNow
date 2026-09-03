package br.com.evento.adapters.in.web.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AtualizarEventoRequest(
        @Size(min = 3, max = 200, message = "O titulo deve ter entre 3 e 200 caracteres")
        String titulo,

        @Size(min = 10, max = 2000, message = "A descricao deve ter entre 10 e 2000 caracteres")
        String descricao,

        @Positive(message = "O numero de vagas deve ser um valor positivo")
        Integer maxVagas
) {}
