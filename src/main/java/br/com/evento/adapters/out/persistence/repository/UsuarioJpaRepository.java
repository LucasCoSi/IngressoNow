package br.com.evento.adapters.out.persistence.repository;

import br.com.evento.adapters.out.persistence.entity.OrganizadorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<OrganizadorJpaEntity, String> {
    Optional<OrganizadorJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
