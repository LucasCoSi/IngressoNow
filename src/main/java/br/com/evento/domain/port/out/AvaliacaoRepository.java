package br.com.evento.domain.port.out;

import br.com.evento.domain.model.entity.Avaliacao;

import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository {
    Avaliacao salvar(Avaliacao avaliacao);
    Optional<Avaliacao> buscarPorId(String id);
    List<Avaliacao> buscarPorQuestionario(String questionarioId);
    boolean existePorQuestionarioEInscricao(String questionarioId, String inscricaoId);
}
