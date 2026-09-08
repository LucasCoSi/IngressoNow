package br.com.evento.application.usecase;

import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.port.out.EventoRepository;

import java.util.List;

/**
 * Caso de uso: listar todos os eventos de um organizador (qualquer status).
 * Consumido pela página admin de gestão — o GET público só retorna
 * PUBLICADO/INSCRICOES_ABERTAS, então o RASCUNHO recém-criado sumiria.
 */
public class ListarEventosOrganizadorUseCase {

    private final EventoRepository eventoRepository;

    public ListarEventosOrganizadorUseCase(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    public List<Evento> executar(String organizadorId) {
        if (organizadorId == null || organizadorId.isBlank()) {
            throw new IllegalArgumentException("O ID do organizador é obrigatório");
        }
        return eventoRepository.buscarPorOrganizador(organizadorId);
    }
}
