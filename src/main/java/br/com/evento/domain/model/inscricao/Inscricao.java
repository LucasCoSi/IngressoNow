package br.com.evento.domain.model.inscricao;

import br.com.evento.domain.model.evento.Evento;
import br.com.evento.domain.model.usuario.Usuario;
import java.util.UUID;

/**
 * Entidade que gerencia o fluxo de inscrição de um participante num evento.
 * Protege as regras de mudança de estado da inscrição.
 */
public class Inscricao {

    private final UUID id;
    private final Evento evento;
    private final Usuario participante;
    private EstadoInscricao estado;

    public Inscricao(Evento evento, Usuario participante) {
        if (evento == null) throw new IllegalArgumentException("Evento é obrigatório para inscrição.");
        if (participante == null) throw new IllegalArgumentException("Participante é obrigatório para inscrição.");
        
        this.id = UUID.randomUUID();
        this.evento = evento;
        this.participante = participante;
        this.estado = EstadoInscricao.PENDENTE;
    }

    public void confirmar() {
        if (this.estado != EstadoInscricao.PENDENTE) {
            throw new IllegalStateException("Apenas inscrições pendentes podem ser confirmadas.");
        }
        // Futuro: Validar limites de vagas utilizando Strategy ou Serviço de Domínio
        this.estado = EstadoInscricao.CONFIRMADA;
    }

    public void cancelar() {
        if (this.estado == EstadoInscricao.CANCELADA) {
            throw new IllegalStateException("A inscrição já está cancelada.");
        }
        this.estado = EstadoInscricao.CANCELADA;
    }

    // Apenas Getters
    public UUID getId() { return id; }
    public Evento getEvento() { return evento; }
    public Usuario getParticipante() { return participante; }
    public EstadoInscricao getEstado() { return estado; }
}
