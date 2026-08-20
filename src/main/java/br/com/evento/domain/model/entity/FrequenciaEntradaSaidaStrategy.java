package br.com.evento.domain.model.entity;

import java.util.List;

public class FrequenciaEntradaSaidaStrategy implements PoliticaFrequencia {
    @Override
    public StatusPresenca calcularStatus(List<CheckIn> checkInsAtividade) {
        if (checkInsAtividade == null || checkInsAtividade.isEmpty()) {
            return StatusPresenca.AUSENTE;
        }
        if (checkInsAtividade.size() % 2 != 0) {
            return StatusPresenca.PENDENTE_SAIDA;
        }
        return StatusPresenca.PRESENTE;
    }
}
