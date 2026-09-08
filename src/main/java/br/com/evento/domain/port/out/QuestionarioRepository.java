package br.com.evento.domain.port.out;

import br.com.evento.domain.model.entity.Questionario;

import java.util.List;
import java.util.Optional;

public interface QuestionarioRepository {
    Questionario salvar(Questionario questionario);
    Optional<Questionario> buscarPorId(String id);
    List<Questionario> buscarPorEvento(String eventoId);
}
