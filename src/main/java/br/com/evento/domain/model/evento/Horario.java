package br.com.evento.domain.model.evento;

import java.time.LocalDateTime;

/**
 * Objeto de Valor que encapsula um período de tempo (início e fim).
 * Garante que a data de fim seja sempre posterior à de início. (ROO-03)
 */
public class Horario {
    
    private final LocalDateTime inicio;
    private final LocalDateTime fim;

    public Horario(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Data de início e fim são obrigatórias.");
        }
        if (fim.isBefore(inicio) || fim.isEqual(inicio)) {
            throw new IllegalArgumentException("Data de fim deve ser posterior à data de início.");
        }
        this.inicio = inicio;
        this.fim = fim;
    }

    public LocalDateTime getInicio() { return inicio; }
    public LocalDateTime getFim() { return fim; }

    public boolean conflitaCom(Horario outroHorario) {
        return this.inicio.isBefore(outroHorario.getFim()) && this.fim.isAfter(outroHorario.getInicio());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Horario horario = (Horario) o;
        return inicio.equals(horario.inicio) && fim.equals(horario.fim);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(inicio, fim);
    }
}
