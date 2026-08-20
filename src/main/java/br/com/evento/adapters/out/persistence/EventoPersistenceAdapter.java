package br.com.evento.adapters.out.persistence;

import br.com.evento.adapters.out.persistence.entity.EventoJpaEntity;
import br.com.evento.adapters.out.persistence.mapper.EventoMapper;
import br.com.evento.adapters.out.persistence.repository.EventoJpaRepository;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.StatusEvento;
import br.com.evento.domain.port.out.EventoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementação da porta de saída EventoRepository.
 * Traduz entre o domínio e o Spring Data JPA usando Mappers.
 */
@Component
public class EventoPersistenceAdapter implements EventoRepository {

    private final EventoJpaRepository jpaRepository;

    public EventoPersistenceAdapter(EventoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Evento salvar(Evento evento) {
        EventoJpaEntity jpa = EventoMapper.toJpa(evento);
        EventoJpaEntity salvo = jpaRepository.save(jpa);
        return EventoMapper.toDomain(salvo);
    }

    @Override
    public Optional<Evento> buscarPorId(String id) {
        return jpaRepository.findById(id)
                .map(EventoMapper::toDomain);
    }

    @Override
    public List<Evento> buscarPorStatus(StatusEvento status) {
        return jpaRepository.findByStatus(status.name())
                .stream()
                .map(EventoMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Evento> buscarPorOrganizador(String organizadorId) {
        return jpaRepository.findByOrganizadorId(organizadorId)
                .stream()
                .map(EventoMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void remover(String id) {
        jpaRepository.deleteById(id);
    }
}
