package br.com.evento.domain.model.vo;

import br.com.evento.domain.exception.DomainException;
import java.time.LocalDateTime;
import java.util.Objects;

public class Periodo {
    private final LocalDateTime inicio;
    private final LocalDateTime fim;

    public Periodo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            throw new DomainException("Datas de início e fim são obrigatórias.");
        }
        if (fim.isBefore(inicio)) {
            throw new DomainException("A data de fim não pode ser anterior à data de início.");
        }
        this.inicio = inicio;
        this.fim = fim;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public boolean conflitaCom(Periodo outro) {
        if (outro == null) return false;
        return this.inicio.isBefore(outro.fim) && outro.inicio.isBefore(this.fim);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Periodo periodo = (Periodo) o;
        return Objects.equals(inicio, periodo.inicio) && Objects.equals(fim, periodo.fim);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inicio, fim);
    }
}
