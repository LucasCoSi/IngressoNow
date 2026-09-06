package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;

import java.util.Objects;
import java.util.UUID;

/**
 * RF-08 — pessoa vinculada a uma atividade com um papel
 * (palestrante, apresentador, responsável, mediador...).
 * Papel é texto livre configurável, sem enum fixo.
 */
public class Pessoa {

    private final String id;
    private final String atividadeId;
    private String nome;
    private String papel;

    public Pessoa(String id, String atividadeId, String nome, String papel) {
        if (id == null || id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }
        if (atividadeId == null || atividadeId.isBlank()) {
            throw new DomainException("A atividade da pessoa é obrigatória.");
        }
        definirNome(nome);
        definirPapel(papel);
        this.atividadeId = atividadeId;
    }

    public Pessoa(String atividadeId, String nome, String papel) {
        this(null, atividadeId, nome, papel);
    }

    public String getId() { return id; }
    public String getAtividadeId() { return atividadeId; }
    public String getNome() { return nome; }
    public String getPapel() { return papel; }

    public void definirNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("O nome da pessoa é obrigatório.");
        }
        this.nome = nome.trim();
    }

    public void definirPapel(String papel) {
        if (papel == null || papel.isBlank()) {
            throw new DomainException("O papel da pessoa é obrigatório.");
        }
        this.papel = papel.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pessoa pessoa = (Pessoa) o;
        return Objects.equals(id, pessoa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
