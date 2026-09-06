package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Pergunta;
import br.com.evento.domain.model.entity.Questionario;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.QuestionarioRepository;

import java.util.List;

public class CriarQuestionarioUseCase {

    private final EventoRepository eventoRepository;
    private final QuestionarioRepository questionarioRepository;

    public CriarQuestionarioUseCase(EventoRepository eventoRepository,
                                    QuestionarioRepository questionarioRepository) {
        this.eventoRepository = eventoRepository;
        this.questionarioRepository = questionarioRepository;
    }

    public Questionario executar(String eventoId, String titulo, List<Pergunta> perguntas) {
        return executar(eventoId, titulo, perguntas, null);
    }

    /** RN-15: política de identificação informada antes do envio. */
    public Questionario executar(String eventoId, String titulo, List<Pergunta> perguntas,
                                 String politicaIdentificacao) {
        eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new DomainException("Evento não encontrado."));
        Questionario questionario = new Questionario(null, eventoId, titulo);
        questionario.definirPoliticaIdentificacao(politicaIdentificacao);
        if (perguntas != null) {
            for (Pergunta pergunta : perguntas) {
                questionario.adicionarPergunta(pergunta);
            }
        }
        if (questionario.getPerguntas().isEmpty()) {
            throw new DomainException("O questionário exige ao menos uma pergunta.");
        }
        return questionarioRepository.salvar(questionario);
    }
}
