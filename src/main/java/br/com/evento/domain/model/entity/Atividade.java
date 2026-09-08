package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.vo.Periodo;

import java.util.Objects;
import java.util.UUID;

public class Atividade {
    private final String id;
    private String titulo;
    private Periodo periodo;
    private String local;
    /** RF-05: tipo configurável (palestra, workshop, painel...), sem categoria fixa. */
    private String tipo;
    /** RF-06: trilha/categoria/espaço para filtrar e agrupar a programação. */
    private String trilha;
    private PoliticaFrequencia politicaFrequencia;
    /** RF-13: limite de vagas da atividade (null = sem limite). */
    private Integer maxVagas;

    public Atividade(String id, String titulo, Periodo periodo, String local) {
        this(id, titulo, periodo, local, null, null);
    }

    public Atividade(String id, String titulo, Periodo periodo, String local, String tipo, String trilha) {
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
        this.tipo = tipo == null || tipo.isBlank() ? null : tipo.trim();
        this.trilha = trilha == null || trilha.isBlank() ? null : trilha.trim();
        this.politicaFrequencia = new FrequenciaUnicaStrategy();
        this.maxVagas = null;
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

    public String getTipo() {
        return tipo;
    }

    public String getTrilha() {
        return trilha;
    }

    public Integer getMaxVagas() {
        return maxVagas;
    }

    /**
     * RF-13: configura o limite de vagas da atividade.
     * null = sem limite. Só faz sentido com `vagasPorAtividade` do evento.
     */
    public void configurarLimiteDeVagas(Integer maxVagas) {
        if (maxVagas != null && maxVagas <= 0) {
            throw new DomainException("O limite de vagas da atividade deve ser positivo.");
        }
        this.maxVagas = maxVagas;
    }

    /**
     * RF-13/RN-06: há vaga para mais uma seleção? Sem limite → sempre true.
     */
    public boolean podeAcomodar(int ocupadas) {
        if (maxVagas == null) return true;
        return ocupadas < maxVagas;
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

    /**
     * RF-19: critério de presença configurável por nome
     * ("UNICA" = check-in único; "ENTRADA_SAIDA" = pares entrada/saída).
     */
    public void definirPoliticaFrequencia(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("O critério de presença é obrigatório.");
        }
        switch (nome.trim().toUpperCase()) {
            case "UNICA" -> this.politicaFrequencia = new FrequenciaUnicaStrategy();
            case "ENTRADA_SAIDA" -> this.politicaFrequencia = new FrequenciaEntradaSaidaStrategy();
            default -> throw new DomainException(
                    "Critério de presença inválido (use UNICA ou ENTRADA_SAIDA).");
        }
    }

    /** Nome do critério vigente (p/ persistência e DTOs). */
    public String getNomePoliticaFrequencia() {
        return (politicaFrequencia instanceof FrequenciaEntradaSaidaStrategy)
                ? "ENTRADA_SAIDA" : "UNICA";
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
