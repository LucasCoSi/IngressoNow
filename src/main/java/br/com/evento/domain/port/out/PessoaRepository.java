package br.com.evento.domain.port.out;

import br.com.evento.domain.model.entity.Pessoa;

import java.util.List;
import java.util.Optional;

public interface PessoaRepository {
    Pessoa salvar(Pessoa pessoa);
    Optional<Pessoa> buscarPorId(String id);
    List<Pessoa> buscarPorAtividade(String atividadeId);
    void remover(String id);
}
