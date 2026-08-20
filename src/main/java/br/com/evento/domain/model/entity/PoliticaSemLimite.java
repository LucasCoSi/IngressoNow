package br.com.evento.domain.model.entity;

/**
 * Strategy concreta — evento sem limite de vagas (aceita qualquer número de inscritos).
 */
public class PoliticaSemLimite implements PoliticaInscricao {

    @Override
    public boolean podeInscrever(int totalInscricoesAtuais) {
        return true;
    }
}
