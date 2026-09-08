package br.com.evento.domain.port.out;

import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.StatusEvento;

import java.util.List;
import java.util.Optional;

/**
 * Porta de saída do domínio para persistência de Eventos.
 * Nenhuma dependência de framework é permitida nesta interface.
 */
public interface EventoRepository {
    Evento salvar(Evento evento);
    Optional<Evento> buscarPorId(String id);
    List<Evento> buscarPorStatus(StatusEvento status);
    List<Evento> buscarPorOrganizador(String organizadorId);
    void remover(String id);
}
