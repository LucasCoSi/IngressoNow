package br.com.evento.adapters.out.persistence;

import br.com.evento.adapters.out.persistence.entity.OrganizadorJpaEntity;
import br.com.evento.adapters.out.persistence.mapper.UsuarioMapper;
import br.com.evento.adapters.out.persistence.repository.UsuarioJpaRepository;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.port.out.UsuarioRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Implementação da porta de saída UsuarioRepository.
 * Traduz entre o domínio e o Spring Data JPA usando Mappers.
 */
@Component
public class UsuarioPersistenceAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository jpaRepository;

    public UsuarioPersistenceAdapter(UsuarioJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        OrganizadorJpaEntity jpa = UsuarioMapper.toJpa(usuario);
        OrganizadorJpaEntity salvo = jpaRepository.save(jpa);
        return UsuarioMapper.toDomain(salvo);
    }

    @Override
    public Optional<Usuario> buscarPorId(String id) {
        return jpaRepository.findById(id)
                .map(UsuarioMapper::toDomain);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(Email email) {
        return jpaRepository.findByEmail(email.getEndereco())
                .map(UsuarioMapper::toDomain);
    }

    @Override
    public boolean existePorEmail(Email email) {
        return jpaRepository.existsByEmail(email.getEndereco());
    }
}
