package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * RF-26/RF-27 — avaliação submetida por um participante.
 * Invariantes protegidas no construtor (ROO-02):
 *  - elegibilidade: inscrição com presença validada (RN-13);
 *  - cada resposta é validada contra sua pergunta (RF-25);
 *  - duplicidade (RN-14) é verificada no use case via repositório.
 */
public class Avaliacao {
    private final String id;
    private final String questionarioId;
    private final String inscricaoId;
    private final List<Resposta> respostas;
    private final LocalDateTime dataSubmissao;

    public Avaliacao(String id, String questionarioId, String inscricaoId,
                     List<Resposta> respostas, LocalDateTime dataSubmissao) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.questionarioId = questionarioId;
        this.inscricaoId = inscricaoId;
        this.respostas = respostas == null ? List.of() : List.copyOf(respostas);
        this.dataSubmissao = dataSubmissao != null ? dataSubmissao : LocalDateTime.now();
    }

    public Avaliacao(Questionario questionario, Inscricao inscricao, List<Resposta> respostas) {
        if (questionario == null) {
            throw new DomainException("O questionário é obrigatório.");
        }
        if (inscricao == null) {
            throw new DomainException("A inscrição é obrigatória.");
        }
        if (!inscricao.isAptoParaAvaliacao()) {
            throw new DomainException("Participante não possui presenças mínimas para avaliar o evento.");
        }
        if (respostas == null || respostas.isEmpty()) {
            throw new DomainException("A avaliação exige ao menos uma resposta.");
        }
        for (Resposta resposta : respostas) {
            Pergunta pergunta = questionario.buscarPergunta(resposta.getPerguntaId());
            pergunta.validarResposta(resposta.getValor());
        }
        this.id = UUID.randomUUID().toString();
        this.questionarioId = questionario.getId();
        this.inscricaoId = inscricao.getId();
        this.respostas = List.copyOf(respostas);
        this.dataSubmissao = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getQuestionarioId() { return questionarioId; }
    public String getInscricaoId() { return inscricaoId; }
    public List<Resposta> getRespostas() { return Collections.unmodifiableList(respostas); }
    public LocalDateTime getDataSubmissao() { return dataSubmissao; }
}
