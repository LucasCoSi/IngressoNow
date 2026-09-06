package br.com.evento.application.usecase;

import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.UsuarioRepository;

import java.util.List;

/**
 * RF-29 — relatório de inscritos por evento (CSV portátil, RF-31).
 * Inclui nome do participante, data de inscrição e nº de atividades selecionadas.
 */
public class GerarRelatorioInscritosUseCase {

    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final UsuarioRepository usuarioRepository;

    public GerarRelatorioInscritosUseCase(EventoRepository eventoRepository,
                                          InscricaoRepository inscricaoRepository,
                                          UsuarioRepository usuarioRepository) {
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public String executarEmCsv(String eventoId) {
        Evento evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));
        List<Inscricao> inscricoes = inscricaoRepository.buscarPorEvento(eventoId);

        StringBuilder csv = new StringBuilder();
        csv.append("InscricaoID,ParticipanteID,ParticipanteNome,EventoID,EventoTitulo,DataInscricao,AtividadesSelecionadas\n");
        for (Inscricao i : inscricoes) {
            String nome = usuarioRepository.buscarPorId(i.getParticipanteId())
                    .map(u -> u.getNome()).orElse(i.getParticipanteId());
            csv.append(i.getId()).append(",")
                    .append(i.getParticipanteId()).append(",")
                    .append(csvEscape(nome)).append(",")
                    .append(i.getEventoId()).append(",")
                    .append(csvEscape(evento.getTitulo())).append(",")
                    .append(i.getDataInscricao()).append(",")
                    .append(i.getAtividadesSelecionadas().size()).append("\n");
        }
        return csv.toString();
    }

    private static String csvEscape(String valor) {
        if (valor == null) return "";
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
