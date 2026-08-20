package br.com.evento.domain.model.entity;

/**
 * Strategy — define a política de controle de vagas de um Evento.
 * Implementações concretas decidem se uma nova inscrição é permitida
 * com base no total atual de inscritos.
 */
public interface PoliticaInscricao {
    boolean podeInscrever(int totalInscricoesAtuais);
}
