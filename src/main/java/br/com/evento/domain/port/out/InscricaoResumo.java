package br.com.evento.domain.port.out;

import java.time.LocalDateTime;

/**
 * RNF-10: projeção leve de inscrição p/ listagens (nome do participante
 * e título do evento resolvidos no próprio SELECT — 1 query, sem N+1).
 */
public record InscricaoResumo(String id, String participanteId, String participanteNome,
                              String eventoId, String eventoTitulo, LocalDateTime dataInscricao) {
}
