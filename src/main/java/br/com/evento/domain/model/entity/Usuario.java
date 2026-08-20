package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;

import java.util.Objects;
import java.util.UUID;

public abstract class Usuario {
    private final String id;
    private String nome;
    private Email email;
    private SenhaHash senha;

    protected Usuario(String id, String nome, Email email, SenhaHash senha) {
        if (id == null || id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }
        
        if (nome == null || nome.trim().isEmpty()) {
            throw new DomainException("O nome do usuário não pode ser vazio.");
        }
        if (email == null) {
            throw new DomainException("O e-mail é obrigatório.");
        }
        if (senha == null) {
            throw new DomainException("A senha é obrigatória.");
        }
        
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Email getEmail() {
        return email;
    }

    public SenhaHash getSenha() {
        return senha;
    }

    public void alterarNome(String novoNome) {
        if (novoNome == null || novoNome.trim().isEmpty()) {
            throw new DomainException("O novo nome não pode ser vazio.");
        }
        this.nome = novoNome;
    }

    public void alterarSenha(SenhaHash novaSenha) {
        if (novaSenha == null) {
            throw new DomainException("A nova senha é obrigatória.");
        }
        this.senha = novaSenha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(id, usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
