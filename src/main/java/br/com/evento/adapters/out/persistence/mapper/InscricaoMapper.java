package br.com.evento.adapters.out.persistence.mapper;

import br.com.evento.adapters.out.persistence.entity.AtividadeJpaEntity;
import br.com.evento.adapters.out.persistence.entity.InscricaoJpaEntity;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Inscricao;

/**
 * Mapper bidirecional entre Inscricao de domínio e InscricaoJpaEntity.
 */
public final class InscricaoMapper {

    private InscricaoMapper() {
        // Classe utilitária — não instanciável
    }

    public static Inscricao toDomain(InscricaoJpaEntity jpa) {
        if (jpa == null) return null;

        Inscricao inscricao = new Inscricao(
                jpa.getId(),
                jpa.getParticipanteId(),
                jpa.getEventoId(),
                jpa.getDataInscricao()
        );

        // Reconstituir atividades selecionadas
        if (jpa.getAtividadesSelecionadas() != null) {
            for (AtividadeJpaEntity atividadeJpa : jpa.getAtividadesSelecionadas()) {
                Atividade atividade = AtividadeMapper.toDomain(atividadeJpa);
                inscricao.selecionarAtividade(atividade);
            }
        }

        return inscricao;
    }

    public static InscricaoJpaEntity toJpa(Inscricao domain) {
        if (domain == null) return null;

        InscricaoJpaEntity jpa = new InscricaoJpaEntity(
                domain.getId(),
                domain.getParticipanteId(),
                domain.getEventoId(),
                domain.getDataInscricao()
        );

        // Mapear atividades selecionadas
        for (Atividade atividade : domain.getAtividadesSelecionadas()) {
            AtividadeJpaEntity atividadeJpa = AtividadeMapper.toJpa(atividade);
            jpa.getAtividadesSelecionadas().add(atividadeJpa);
        }

        return jpa;
    }
}
