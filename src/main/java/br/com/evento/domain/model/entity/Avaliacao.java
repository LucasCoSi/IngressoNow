package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Avaliacao {
    private final String id;
    private final String questionarioId;
    private final String inscricaoId;
    private final List<Resposta> respostas;
    private final LocalDateTime dataSubmissao;

    public Avaliacao(String id, String questionarioId, String inscricaoId, List<Resposta> respostas, LocalDateTime dataSubmissao) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.questionarioId = questionarioId;
        this.inscricaoId = inscricaoId;
        this.respostas = respostas;
        this.dataSubmissao = dataSubmissao != null ? dataSubmissao : LocalDateTime.now();
    }

    public Avaliacao(Questionario questionario, Inscricao inscricao, List<Resposta> respostas) {
        if (!inscricao.isAptoParaAvaliacao()) {
            throw new DomainException("Participante não possui presenças mínimas para avaliar o evento.");
        }
        this.id = UUID.randomUUID().toString();
        this.questionarioId = questionario.getId();
        this.inscricaoId = inscricao.getId();
        this.respostas = respostas;
        this.dataSubmissao = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getQuestionarioId() { return questionarioId; }
    public String getInscricaoId() { return inscricaoId; }
    public List<Resposta> getRespostas() { return respostas; }
    public LocalDateTime getDataSubmissao() { return dataSubmissao; }
}
