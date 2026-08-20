package br.com.evento.domain.model.entity;

import java.util.List;

public interface PoliticaFrequencia {
    StatusPresenca calcularStatus(List<CheckIn> checkInsAtividade);
}
