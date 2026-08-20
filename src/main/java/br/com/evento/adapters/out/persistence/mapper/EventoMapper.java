package br.com.evento.adapters.out.persistence.mapper;

import br.com.evento.adapters.out.persistence.entity.AtividadeJpaEntity;
import br.com.evento.adapters.out.persistence.entity.EventoJpaEntity;
import br.com.evento.adapters.out.persistence.entity.OrganizadorJpaEntity;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.StatusEvento;

import java.util.List;

/**
 * Mapper bidirecional entre Evento de domínio e EventoJpaEntity.
 * Reconstitui o agregado completo (Evento + Atividades + Organizador) a partir do banco.
 */
public final class EventoMapper {

    private EventoMapper() {
        // Classe utilitária — não instanciável
    }

    public static Evento toDomain(EventoJpaEntity jpa) {
        if (jpa == null) return null;

        Organizador organizador = UsuarioMapper.toDomain(jpa.getOrganizador());

        Evento evento = new Evento(jpa.getId(), jpa.getTitulo(), jpa.getDescricao(), organizador);

        // Restaurar as atividades reconstituindo cada uma via mapper
        List<AtividadeJpaEntity> atividadesJpa = jpa.getAtividades();
        if (atividadesJpa != null) {
            for (AtividadeJpaEntity atividadeJpa : atividadesJpa) {
                Atividade atividade = AtividadeMapper.toDomain(atividadeJpa);
                evento.adicionarAtividade(atividade);
            }
        }

        // Restaurar o status salvo no banco avançando o State do domínio
        restaurarStatus(evento, StatusEvento.valueOf(jpa.getStatus()));

        return evento;
    }

    public static EventoJpaEntity toJpa(Evento domain) {
        if (domain == null) return null;

        OrganizadorJpaEntity organizadorJpa = UsuarioMapper.toJpa(domain.getOrganizador());

        EventoJpaEntity jpa = new EventoJpaEntity(
                domain.getId(),
                domain.getTitulo(),
                domain.getDescricao(),
                domain.getStatus().name(),
                organizadorJpa
        );

        // Mapear atividades e vincular ao evento JPA
        for (Atividade atividade : domain.getAtividades()) {
            AtividadeJpaEntity atividadeJpa = AtividadeMapper.toJpa(atividade);
            atividadeJpa.setEvento(jpa);
            jpa.getAtividades().add(atividadeJpa);
        }

        return jpa;
    }

    /**
     * Avança o estado do evento recém-reconstituído até o status salvo no banco.
     * Necessário porque o construtor de Evento sempre inicia em RASCUNHO.
     */
    private static void restaurarStatus(Evento evento, StatusEvento statusAlvo) {
        if (evento.getStatus() == statusAlvo) return;

        switch (statusAlvo) {
            case PUBLICADO -> evento.publicar();
            case INSCRICOES_ABERTAS -> {
                evento.publicar();
                evento.abrirInscricoes();
            }
            case ENCERRADO -> evento.encerrar();
            default -> { /* RASCUNHO — já é o estado inicial */ }
        }
    }
}
