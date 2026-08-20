package br.com.evento.adapters.out.persistence.mapper;

import br.com.evento.adapters.out.persistence.entity.AtividadeJpaEntity;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.vo.Periodo;

/**
 * Mapper bidirecional entre Atividade de domínio e AtividadeJpaEntity.
 * O Periodo (Value Object) é decomposto em duas colunas (inicio/fim) no banco.
 */
public final class AtividadeMapper {

    private AtividadeMapper() {
        // Classe utilitária — não instanciável
    }

    public static Atividade toDomain(AtividadeJpaEntity jpa) {
        if (jpa == null) return null;
        Periodo periodo = new Periodo(jpa.getInicio(), jpa.getFim());
        return new Atividade(jpa.getId(), jpa.getTitulo(), periodo, jpa.getLocal());
    }

    public static AtividadeJpaEntity toJpa(Atividade domain) {
        if (domain == null) return null;
        return new AtividadeJpaEntity(
                domain.getId(),
                domain.getTitulo(),
                domain.getPeriodo().getInicio(),
                domain.getPeriodo().getFim(),
                domain.getLocal()
        );
    }
}
