package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Avaliacao;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Questionario;
import br.com.evento.domain.model.entity.Resposta;
import br.com.evento.domain.port.out.AvaliacaoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.QuestionarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubmeterAvaliacaoUseCase {

    private final InscricaoRepository inscricaoRepository;
    private final QuestionarioRepository questionarioRepository;
    private final AvaliacaoRepository avaliacaoRepository;

    public SubmeterAvaliacaoUseCase(InscricaoRepository inscricaoRepository,
                                    QuestionarioRepository questionarioRepository,
                                    AvaliacaoRepository avaliacaoRepository) {
        this.inscricaoRepository = inscricaoRepository;
        this.questionarioRepository = questionarioRepository;
        this.avaliacaoRepository = avaliacaoRepository;
    }

    public Avaliacao executar(String questionarioId, String inscricaoId, List<Resposta> respostas) {
        Questionario questionario = questionarioRepository.buscarPorId(questionarioId)
                .orElseThrow(() -> new DomainException("Questionário não encontrado."));
        Inscricao inscricao = inscricaoRepository.buscarPorId(inscricaoId)
                .orElseThrow(() -> new DomainException("Inscrição não encontrada."));
        if (avaliacaoRepository.existePorQuestionarioEInscricao(questionarioId, inscricaoId)) {
            throw new DomainException("Participante já respondeu este questionário.");
        }
        Avaliacao avaliacao = new Avaliacao(questionario, inscricao, respostas);
        Avaliacao salva = avaliacaoRepository.salvar(avaliacao);
        questionario.congelar();
        questionarioRepository.salvar(questionario);
        return salva;
    }
}
