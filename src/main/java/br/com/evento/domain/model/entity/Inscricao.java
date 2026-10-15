package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado Inscrição — vincula um Participante a um Evento e gerencia
 * a seleção de atividades de interesse (RF-16).
 * A validação de conflito de horário entre atividades selecionadas
 * é responsabilidade desta entidade.
 */
public class Inscricao {

    private final String id;
    private final String participanteId;
    private final String eventoId;
    private final LocalDateTime dataInscricao;
    private final List<Atividade> atividadesSelecionadas;
    private final List<CheckIn> checkIns;

    public Inscricao(String participanteId, String eventoId) {
        this(UUID.randomUUID().toString(), participanteId, eventoId, LocalDateTime.now());
    }

    /**
     * Construtor de reconstituição — usado pelo Mapper para restaurar do banco.
     */
    public Inscricao(String id, String participanteId, String eventoId, LocalDateTime dataInscricao) {
        if (id == null || id.trim().isEmpty()) {
            throw new DomainException("O id da inscrição é obrigatório.");
        }
        if (participanteId == null || participanteId.trim().isEmpty()) {
            throw new DomainException("O participante é obrigatório para a inscrição.");
        }
        if (eventoId == null || eventoId.trim().isEmpty()) {
            throw new DomainException("O evento é obrigatório para a inscrição.");
        }
        if (dataInscricao == null) {
            throw new DomainException("A data de inscrição é obrigatória.");
        }

        this.id = id;
        this.participanteId = participanteId;
        this.eventoId = eventoId;
        this.dataInscricao = dataInscricao;
        this.atividadesSelecionadas = new ArrayList<>();
        this.checkIns = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getParticipanteId() {
        return participanteId;
    }

    public String getEventoId() {
        return eventoId;
    }

    public LocalDateTime getDataInscricao() {
        return dataInscricao;
    }

    public List<Atividade> getAtividadesSelecionadas() {
        return Collections.unmodifiableList(atividadesSelecionadas);
    }

    /**
     * Seleciona uma atividade de interesse para a agenda do participante.
     * Lança DomainException se a atividade conflitar com outra já selecionada (RF-17).
     */
    public void selecionarAtividade(Atividade atividade) {
        if (atividade == null) {
            throw new DomainException("Atividade não pode ser nula.");
        }
        for (Atividade selecionada : atividadesSelecionadas) {
            if (selecionada.getId().equals(atividade.getId())) {
                throw new DomainException("A atividade '" + atividade.getTitulo() + "' já está selecionada.");
            }
            if (selecionada.conflitaCom(atividade)) {
                throw new DomainException(
                        "A atividade '" + atividade.getTitulo() +
                        "' conflita com a atividade '" + selecionada.getTitulo() + "' já selecionada."
                );
            }
        }
        this.atividadesSelecionadas.add(atividade);
    }

    /**
     * Remove uma atividade da seleção do participante.
     */
    public void removerAtividade(String atividadeId) {
        boolean removido = atividadesSelecionadas.removeIf(a -> a.getId().equals(atividadeId));
        if (!removido) {
            throw new DomainException("Atividade não encontrada na seleção.");
        }
    }

    /**
     * Gera a agenda do participante com as atividades ordenadas cronologicamente.
     */
    public AgendaDoParticipante gerarAgenda() {
        return new AgendaDoParticipante(participanteId, atividadesSelecionadas);
    }

    public void registrarCheckIn(Atividade atividade, LocalDateTime momento) {
        registrarCheckIn(atividade, momento, CheckIn.Origem.QR_CODE, null);
    }

    /** RF-22 — lançamento manual alternativo com autoria (RN-12). */
    public void registrarCheckInManual(Atividade atividade, LocalDateTime momento, String responsavelId) {
        registrarCheckIn(atividade, momento, CheckIn.Origem.MANUAL, responsavelId);
    }

    private void registrarCheckIn(Atividade atividade, LocalDateTime momento,
                                  CheckIn.Origem origem, String responsavelId) {
        if (atividade == null) {
            throw new DomainException("Atividade não pode ser nula.");
        }
        boolean selecionada = atividadesSelecionadas.stream()
                .anyMatch(a -> a.getId().equals(atividade.getId()));
        if (!selecionada) {
            throw new DomainException("Participante não selecionou esta atividade em sua inscrição.");
        }
        // RNF-11: repetição acidental não pode duplicar presença no check-in único.
        if (atividade.getPoliticaFrequencia() instanceof FrequenciaUnicaStrategy
                && checkIns.stream().anyMatch(c -> c.getAtividadeId().equals(atividade.getId()))) {
            throw new DomainException("Presença já registrada para esta atividade.");
        }
        this.checkIns.add(new CheckIn(atividade.getId(), momento, origem, responsavelId));
    }

    public List<CheckIn> getCheckIns(String atividadeId) {
        return checkIns.stream()
                .filter(c -> c.getAtividadeId().equals(atividadeId))
                .toList();
    }

    public List<CheckIn> getTodosCheckIns() {
        return Collections.unmodifiableList(checkIns);
    }

    public StatusPresenca calcularPresenca(Atividade atividade) {
        if (!atividadesSelecionadas.contains(atividade)) {
            return StatusPresenca.AUSENTE;
        }
        return atividade.getPoliticaFrequencia().calcularStatus(getCheckIns(atividade.getId()));
    }

    /**
     * Regra RN-13, RF-26: O participante só pode responder avaliação
     * se tiver presenças mínimas validadas. Exemplo: pelo menos 1 check-in válido.
     */
    public boolean isAptoParaAvaliacao() {
        long presencasValidas = atividadesSelecionadas.stream()
                .map(this::calcularPresenca)
                .filter(s -> s == StatusPresenca.PRESENTE)
                .count();
        return presencasValidas > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inscricao inscricao = (Inscricao) o;
        return Objects.equals(id, inscricao.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
