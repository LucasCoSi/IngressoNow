package br.com.evento.adapters.out.persistence.repository;

import br.com.evento.adapters.out.persistence.entity.InscricaoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscricaoJpaRepository extends JpaRepository<InscricaoJpaEntity, String> {
    List<InscricaoJpaEntity> findByParticipanteId(String participanteId);
    List<InscricaoJpaEntity> findByEventoId(String eventoId);
    int countByEventoId(String eventoId);
    boolean existsByParticipanteIdAndEventoId(String participanteId, String eventoId);
}
