package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;

/**
 * Strategy concreta — evento com vagas limitadas.
 * Rejeita novas inscrições quando o total atinge o máximo configurado.
 */
public class PoliticaComLimiteDeVagas implements PoliticaInscricao {

    private final int maxVagas;

    public PoliticaComLimiteDeVagas(int maxVagas) {
        if (maxVagas <= 0) {
            throw new DomainException("O limite de vagas deve ser positivo.");
        }
        this.maxVagas = maxVagas;
    }

    public int getMaxVagas() {
        return maxVagas;
    }

    @Override
    public boolean podeInscrever(int totalInscricoesAtuais) {
        return totalInscricoesAtuais < maxVagas;
    }
}
