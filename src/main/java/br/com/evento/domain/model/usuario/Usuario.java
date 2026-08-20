package br.com.evento.domain.model.usuario;

import java.util.UUID;

/**
 * Entidade que representa um Usuário do sistema.
 * Demonstra encapsulamento e proteção de invariantes (ROO-02).
 */
public class Usuario {

    private final UUID id;
    private String nome;
    private final Email email; // Usando Objeto de Valor (ROO-03)
    private String senhaHash;
    private boolean ativo;

    public Usuario(String nome, Email email, String senhaHash) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (senhaHash == null || senhaHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Senha é obrigatória.");
        }
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.ativo = true;
    }
    
    // Construtor para reconstrução via repositório (JPA/JDBC)
    protected Usuario(UUID id, String nome, Email email, String senhaHash, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.ativo = ativo;
    }

    // Comportamentos de domínio em vez de setters vazios
    public void alterarNome(String novoNome) {
        if (novoNome == null || novoNome.trim().isEmpty()) {
            throw new IllegalArgumentException("Novo nome não pode ser vazio.");
        }
        this.nome = novoNome;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    // Apenas Getters (Sem setters públicos)
    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public Email getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public boolean isAtivo() { return ativo; }
}
