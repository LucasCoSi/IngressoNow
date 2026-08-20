package br.com.evento.adapters.out.persistence.entity;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("ORGANIZADOR")
public class OrganizadorJpaEntity extends UsuarioJpaEntity {

    protected OrganizadorJpaEntity() {
        // Construtor padrão exigido pelo JPA
    }

    public OrganizadorJpaEntity(String id, String nome, String email, String senhaHash) {
        super(id, nome, email, senhaHash);
    }
}
