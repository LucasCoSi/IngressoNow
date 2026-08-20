package br.com.evento.adapters.out.persistence.repository;

import br.com.evento.adapters.out.persistence.entity.EventoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoJpaRepository extends JpaRepository<EventoJpaEntity, String> {
    List<EventoJpaEntity> findByStatus(String status);
    List<EventoJpaEntity> findByOrganizadorId(String organizadorId);
}
