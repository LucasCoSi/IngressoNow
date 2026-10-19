package br.com.evento.application.usecase;

import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.StatusEvento;
import br.com.evento.domain.port.out.EventoRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Caso de uso: listar TODOS os eventos, qualquer status (§3: o administrador
 * consulta dados de todos os eventos). Uso interno do desktop do admin;
 * sem rota pública dedicada.
 */
public class ListarTodosEventosUseCase {

    private final EventoRepository eventoRepository;

    public ListarTodosEventosUseCase(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    public List<Evento> executar() {
        List<Evento> resultado = new ArrayList<>();
        for (StatusEvento status : StatusEvento.values()) {
            resultado.addAll(eventoRepository.buscarPorStatus(status));
        }
        return resultado;
    }
}
