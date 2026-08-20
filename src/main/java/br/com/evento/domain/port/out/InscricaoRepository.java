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
    boolean existePorParticipanteEEvento(String participanteId, String eventoId);
}
