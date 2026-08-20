package br.com.evento.domain.model.entity;

import java.util.List;

public class FrequenciaUnicaStrategy implements PoliticaFrequencia {
    @Override
    public StatusPresenca calcularStatus(List<CheckIn> checkInsAtividade) {
        if (checkInsAtividade == null || checkInsAtividade.isEmpty()) {
            return StatusPresenca.AUSENTE;
        }
        return StatusPresenca.PRESENTE;
    }
}
