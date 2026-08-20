package br.com.evento.adapters.out.persistence.mapper;

import br.com.evento.adapters.out.persistence.entity.OrganizadorJpaEntity;
import br.com.evento.adapters.out.persistence.entity.ParticipanteJpaEntity;
import br.com.evento.adapters.out.persistence.entity.UsuarioJpaEntity;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Participante;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.SenhaHash;

/**
 * Mapper bidirecional entre entidades de domínio (Usuario/Organizador/Participante)
 * e entidades JPA. Polimórfico — detecta o tipo concreto automaticamente.
 */
public final class UsuarioMapper {

    private UsuarioMapper() {
        // Classe utilitária — não instanciável
    }

    // ──────────────────── JPA → Domínio ────────────────────

    public static Usuario toDomain(UsuarioJpaEntity jpa) {
        if (jpa == null) return null;
        if (jpa instanceof OrganizadorJpaEntity org) {
            return toDomain(org);
        }
        if (jpa instanceof ParticipanteJpaEntity part) {
            return toDomainParticipante(part);
        }
        throw new IllegalArgumentException("Tipo de usuário JPA não suportado: " + jpa.getClass().getSimpleName());
    }

    public static Organizador toDomain(OrganizadorJpaEntity jpa) {
        if (jpa == null) return null;
        return new Organizador(
                jpa.getId(),
                jpa.getNome(),
                new Email(jpa.getEmail()),
                new SenhaHash(jpa.getSenhaHash())
        );
    }

    public static Participante toDomainParticipante(ParticipanteJpaEntity jpa) {
        if (jpa == null) return null;
        return new Participante(
                jpa.getId(),
                jpa.getNome(),
                new Email(jpa.getEmail()),
                new SenhaHash(jpa.getSenhaHash())
        );
    }

    // ──────────────────── Domínio → JPA ────────────────────

    public static UsuarioJpaEntity toJpa(Usuario domain) {
        if (domain == null) return null;
        if (domain instanceof Organizador org) {
            return toJpaOrganizador(org);
        }
        if (domain instanceof Participante part) {
            return toJpaParticipante(part);
        }
        throw new IllegalArgumentException("Tipo de usuário não suportado para mapeamento JPA: " + domain.getClass().getSimpleName());
    }

    public static OrganizadorJpaEntity toJpaOrganizador(Organizador domain) {
        if (domain == null) return null;
        return new OrganizadorJpaEntity(
                domain.getId(),
                domain.getNome(),
                domain.getEmail().getEndereco(),
                domain.getSenha().getHash()
        );
    }

    public static ParticipanteJpaEntity toJpaParticipante(Participante domain) {
        if (domain == null) return null;
        return new ParticipanteJpaEntity(
                domain.getId(),
                domain.getNome(),
                domain.getEmail().getEndereco(),
                domain.getSenha().getHash()
        );
    }

    // Alias de conveniência para manter compatibilidade com EventoMapper
    public static OrganizadorJpaEntity toJpa(Organizador domain) {
        return toJpaOrganizador(domain);
    }
}
