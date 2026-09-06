package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Pessoa;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.PessoaRepository;

/**
 * RF-08 — vincula pessoa com papel a uma atividade do evento.
 */
public class VincularPessoaUseCase {

    private final EventoRepository eventoRepository;
    private final PessoaRepository pessoaRepository;

    public VincularPessoaUseCase(EventoRepository eventoRepository, PessoaRepository pessoaRepository) {
        this.eventoRepository = eventoRepository;
        this.pessoaRepository = pessoaRepository;
    }

    public Pessoa executar(String eventoId, String atividadeId, String nome, String papel) {
        var evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new DomainException("Evento não encontrado com id: " + eventoId));
        boolean existe = evento.getAtividades().stream().anyMatch(a -> a.getId().equals(atividadeId));
        if (!existe) {
            throw new DomainException("Atividade não encontrada no evento.");
        }
        return pessoaRepository.salvar(new Pessoa(atividadeId, nome, papel));
    }

    public void remover(String pessoaId) {
        pessoaRepository.buscarPorId(pessoaId)
                .orElseThrow(() -> new DomainException("Pessoa não encontrada."));
        pessoaRepository.remover(pessoaId);
    }
}
