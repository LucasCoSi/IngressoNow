package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Avaliacao;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Questionario;
import br.com.evento.domain.model.entity.Resposta;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubmeterAvaliacaoUseCase {

    private final InscricaoRepository inscricaoRepository;
    private final EventoRepository eventoRepository;
    // Supomos que temos um QuestionarioRepository e AvaliacaoRepository 
    // Mocks simulados aqui para simplificação da camada de aplicação

    public SubmeterAvaliacaoUseCase(InscricaoRepository inscricaoRepository, EventoRepository eventoRepository) {
        this.inscricaoRepository = inscricaoRepository;
        this.eventoRepository = eventoRepository;
    }

    public Avaliacao executar(String inscricaoId, Questionario questionario, List<Resposta> respostas) {
        Inscricao inscricao = inscricaoRepository.buscarPorId(inscricaoId)
                .orElseThrow(() -> new DomainException("Inscrição não encontrada."));
                
        // Validação da invariante de negócio (RF-26) é garantida no construtor da Avaliacao
        Avaliacao avaliacao = new Avaliacao(questionario, inscricao, respostas);
        
        // Aqui o avaliacaoRepository.salvar(avaliacao) seria chamado
        return avaliacao;
    }
}
