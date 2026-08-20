package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Evento {
    private final String id;
    private String titulo;
    private String descricao;
    private StatusEvento status;
    private final List<Atividade> atividades;
    private final Organizador organizador;
    private Integer maxVagas;

    public Evento(String id, String titulo, String descricao, Organizador organizador) {
        if (id == null || id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }

        if (titulo == null || titulo.trim().isEmpty()) {
            throw new DomainException("O título do evento é obrigatório.");
        }
        if (organizador == null) {
            throw new DomainException("O organizador do evento é obrigatório.");
        }

        this.titulo = titulo;
        this.descricao = descricao;
        this.organizador = organizador;
        this.status = StatusEvento.RASCUNHO;
        this.atividades = new ArrayList<>();
        this.maxVagas = null;
    }

    public Evento(String titulo, String descricao, Organizador organizador) {
        this(null, titulo, descricao, organizador);
    }

    public String getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public StatusEvento getStatus() {
        return status;
    }

    public Organizador getOrganizador() {
        return organizador;
    }

    public Integer getMaxVagas() {
        return maxVagas;
    }

    /**
     * Configura o limite de vagas do evento.
     * null = sem limite (PoliticaSemLimite).
     */
    public void configurarLimiteDeVagas(Integer maxVagas) {
        if (maxVagas != null && maxVagas <= 0) {
            throw new DomainException("O limite de vagas deve ser positivo.");
        }
        this.maxVagas = maxVagas;
    }

    /**
     * Retorna a estratégia de inscrição derivada do maxVagas (Strategy Pattern).
     */
    public PoliticaInscricao getPoliticaInscricao() {
        if (maxVagas == null) {
            return new PoliticaSemLimite();
        }
        return new PoliticaComLimiteDeVagas(maxVagas);
    }

    public List<Atividade> getAtividades() {
        return Collections.unmodifiableList(atividades);
    }

    public void publicar() {
        if (this.status != StatusEvento.RASCUNHO) {
            throw new DomainException("Apenas eventos em rascunho podem ser publicados.");
        }
        if (this.atividades.isEmpty()) {
            throw new DomainException("O evento precisa ter pelo menos uma atividade para ser publicado.");
        }
        this.status = StatusEvento.PUBLICADO;
    }

    public void abrirInscricoes() {
        if (this.status != StatusEvento.PUBLICADO) {
            throw new DomainException("Inscrições só podem ser abertas para eventos publicados.");
        }
        this.status = StatusEvento.INSCRICOES_ABERTAS;
    }

    public void encerrar() {
        if (this.status == StatusEvento.ENCERRADO) {
            throw new DomainException("O evento já está encerrado.");
        }
        this.status = StatusEvento.ENCERRADO;
    }

    public void adicionarAtividade(Atividade novaAtividade) {
        if (novaAtividade == null) {
            throw new DomainException("Atividade não pode ser nula.");
        }
        for (Atividade atividadeExistente : atividades) {
            if (atividadeExistente.conflitaCom(novaAtividade) && atividadeExistente.getLocal().equals(novaAtividade.getLocal())) {
                throw new DomainException("Conflito de horário e local com outra atividade do evento.");
            }
        }
        this.atividades.add(novaAtividade);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Evento evento = (Evento) o;
        return Objects.equals(id, evento.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
