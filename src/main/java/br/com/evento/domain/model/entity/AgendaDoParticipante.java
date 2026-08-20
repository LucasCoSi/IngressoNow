package br.com.evento.domain.model.entity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agenda do Participante (RF-17) — apresenta as atividades selecionadas
 * ordenadas cronologicamente pelo horário de início.
 */
public class AgendaDoParticipante {

    private final String participanteId;
    private final List<Atividade> atividades;

    public AgendaDoParticipante(String participanteId, List<Atividade> atividades) {
        this.participanteId = participanteId;
        this.atividades = atividades.stream()
                .sorted(Comparator.comparing(a -> a.getPeriodo().getInicio()))
                .collect(Collectors.toList());
    }

    public String getParticipanteId() {
        return participanteId;
    }

    public List<Atividade> getAtividades() {
        return Collections.unmodifiableList(atividades);
    }

    public boolean estaVazia() {
        return atividades.isEmpty();
    }

    public int totalAtividades() {
        return atividades.size();
    }
}
