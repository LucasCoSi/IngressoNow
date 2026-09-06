package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Participante;
import br.com.evento.domain.model.entity.PoliticaInscricao;
import br.com.evento.domain.model.entity.StatusEvento;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.UsuarioRepository;

public class InscreverParticipanteUseCase {

    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InscricaoRepository inscricaoRepository;

    public InscreverParticipanteUseCase(EventoRepository eventoRepository,
                                        UsuarioRepository usuarioRepository,
                                        InscricaoRepository inscricaoRepository) {
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.inscricaoRepository = inscricaoRepository;
    }

    public Inscricao executar(String participanteId, String eventoId) {
        // Valida se participante existe
        var usuario = usuarioRepository.buscarPorId(participanteId)
                .orElseThrow(() -> new DomainException("Participante não encontrado."));
        if (!(usuario instanceof Participante)) {
            throw new DomainException("Apenas participantes podem se inscrever.");
        }

        // Valida se evento existe
        Evento evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new DomainException("Evento não encontrado."));

        // RN-04: inscrições só em evento publicado/aberto
        if (evento.getStatus() != StatusEvento.PUBLICADO
                && evento.getStatus() != StatusEvento.INSCRICOES_ABERTAS) {
            throw new DomainException("Inscrições abertas apenas para eventos publicados.");
        }

        // Valida se já inscrito
        if (inscricaoRepository.existePorParticipanteEEvento(participanteId, eventoId)) {
            throw new DomainException("Participante já inscrito neste evento.");
        }

        // Valida limite de vagas (Strategy)
        PoliticaInscricao politica = evento.getPoliticaInscricao();
        int inscricoesAtuais = inscricaoRepository.contarPorEvento(eventoId);
        if (!politica.podeInscrever(inscricoesAtuais)) {
            throw new DomainException("Não há vagas disponíveis para este evento.");
        }

        Inscricao inscricao = new Inscricao(participanteId, eventoId);
        Inscricao salva = inscricaoRepository.salvar(inscricao);
        // RF-13: evento pode exigir escolha de atividades — informa na resposta
        // (a seleção em si é via POST .../atividades/{atId}; sem escolha, a agenda fica vazia)
        return salva;
    }
}
