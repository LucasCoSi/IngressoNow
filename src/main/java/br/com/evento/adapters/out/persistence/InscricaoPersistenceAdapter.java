package br.com.evento.adapters.out.persistence;

import br.com.evento.adapters.out.persistence.entity.InscricaoJpaEntity;
import br.com.evento.adapters.out.persistence.mapper.InscricaoMapper;
import br.com.evento.adapters.out.persistence.repository.InscricaoJpaRepository;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.port.out.InscricaoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementação da porta de saída InscricaoRepository.
 */
@Component
public class InscricaoPersistenceAdapter implements InscricaoRepository {

    private final InscricaoJpaRepository jpaRepository;

    public InscricaoPersistenceAdapter(InscricaoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Inscricao salvar(Inscricao inscricao) {
        InscricaoJpaEntity jpa = InscricaoMapper.toJpa(inscricao);
        InscricaoJpaEntity salvo = jpaRepository.save(jpa);
        return InscricaoMapper.toDomain(salvo);
    }

    @Override
    public Optional<Inscricao> buscarPorId(String id) {
        return jpaRepository.findById(id).map(InscricaoMapper::toDomain);
    }

    @Override
    public List<Inscricao> buscarPorParticipante(String participanteId) {
        return jpaRepository.findByParticipanteId(participanteId).stream()
                .map(InscricaoMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Inscricao> buscarPorEvento(String eventoId) {
        return jpaRepository.findByEventoId(eventoId).stream()
                .map(InscricaoMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int contarPorEvento(String eventoId) {
        return jpaRepository.countByEventoId(eventoId);
    }

    @Override
    public boolean existePorParticipanteEEvento(String participanteId, String eventoId) {
        return jpaRepository.existsByParticipanteIdAndEventoId(participanteId, eventoId);
    }
}
