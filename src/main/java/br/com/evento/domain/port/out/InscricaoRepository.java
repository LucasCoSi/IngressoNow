package br.com.evento.domain.port.out;

import br.com.evento.domain.model.entity.Inscricao;

import java.util.List;
import java.util.Optional;

/**
 * Porta de saída do domínio para persistência de Inscrições.
 */
public interface InscricaoRepository {
    Inscricao salvar(Inscricao inscricao);
    Optional<Inscricao> buscarPorId(String id);
    List<Inscricao> buscarPorParticipante(String participanteId);
    List<Inscricao> buscarPorEvento(String eventoId);
    int contarPorEvento(String eventoId);
    /** RF-13: quantas inscrições do evento selecionaram a atividade. */
    int contarPorAtividade(String eventoId, String atividadeId);
    /**
     * RNF-10: listagem leve (1 query com JOINs) p/ telas e CSVs — evita
     * reconstituir o agregado completo por linha (N+1).
     */
    List<InscricaoResumo> listarResumidoPorEvento(String eventoId);
    List<InscricaoResumo> listarResumidoPorParticipante(String participanteId);
    boolean existePorParticipanteEEvento(String participanteId, String eventoId);
    void remover(String id);
}
