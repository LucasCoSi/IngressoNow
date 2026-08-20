package br.com.evento.adapters.out.persistence.entity;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("PARTICIPANTE")
public class ParticipanteJpaEntity extends UsuarioJpaEntity {

    protected ParticipanteJpaEntity() {
        // Construtor padrão exigido pelo JPA
    }

    public ParticipanteJpaEntity(String id, String nome, String email, String senhaHash) {
        super(id, nome, email, senhaHash);
    }
}
