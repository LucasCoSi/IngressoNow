package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.vo.Periodo;

import java.util.Objects;
import java.util.UUID;

public class Atividade {
    private final String id;
    private String titulo;
    private final Periodo periodo;
    private final String local;
    private PoliticaFrequencia politicaFrequencia;

    public Atividade(String id, String titulo, Periodo periodo, String local) {
        if (id == null || id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }

        if (titulo == null || titulo.trim().isEmpty()) {
            throw new DomainException("O título da atividade é obrigatório.");
        }
        if (periodo == null) {
            throw new DomainException("O período da atividade é obrigatório.");
        }
        if (local == null || local.trim().isEmpty()) {
            throw new DomainException("O local da atividade é obrigatório.");
        }

        this.titulo = titulo;
        this.periodo = periodo;
        this.local = local;
        this.politicaFrequencia = new FrequenciaUnicaStrategy();
    }

    public Atividade(String titulo, Periodo periodo, String local) {
        this(null, titulo, periodo, local);
    }

    public String getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public Periodo getPeriodo() {
        return periodo;
    }

    public String getLocal() {
        return local;
    }

    public PoliticaFrequencia getPoliticaFrequencia() {
        return politicaFrequencia;
    }

    public void setPoliticaFrequencia(PoliticaFrequencia politicaFrequencia) {
        if (politicaFrequencia == null) {
            throw new IllegalArgumentException("Política de frequência não pode ser nula");
        }
        this.politicaFrequencia = politicaFrequencia;
    }

    public void remarcar(Periodo novoPeriodo, String novoLocal) {
        if (novoPeriodo == null) {
            throw new DomainException("O novo período é obrigatório.");
        }
        if (novoLocal == null || novoLocal.trim().isEmpty()) {
            throw new DomainException("O novo local é obrigatório.");
        }
        this.periodo = novoPeriodo;
        this.local = novoLocal;
    }

    public boolean conflitaCom(Atividade outra) {
        if (outra == null) return false;
        return this.periodo.conflitaCom(outra.getPeriodo());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Atividade atividade = (Atividade) o;
        return Objects.equals(id, atividade.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
