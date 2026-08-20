package br.com.evento.application.usecase;

import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GerarRelatorioPresencaUseCase {

    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;

    public GerarRelatorioPresencaUseCase(EventoRepository eventoRepository, InscricaoRepository inscricaoRepository) {
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
    }

    public String executarEmCsv(String eventoId) {
        Evento evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));
                
        List<Inscricao> inscricoes = inscricaoRepository.buscarPorEvento(eventoId);
        
        StringBuilder csv = new StringBuilder();
        csv.append("InscricaoID,ParticipanteID,AtividadeID,StatusPresenca\n");
        
        for (Inscricao inscricao : inscricoes) {
            for (Atividade atividade : evento.getAtividades()) {
                // Relatório apenas de atividades em que o usuário se inscreveu
                if (inscricao.getAtividadesSelecionadas().contains(atividade)) {
                    csv.append(inscricao.getId()).append(",")
                       .append(inscricao.getParticipanteId()).append(",")
                       .append(atividade.getId()).append(",")
                       .append(inscricao.calcularPresenca(atividade).name()).append("\n");
                }
            }
        }
        
        return csv.toString();
    }
}
