package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * RF-32 (D) — critério de elegibilidade para certificado por evento (RN-16).
 * Combina: percentual mínimo de presença + conjunto de atividades
 * obrigatórias. Tudo opcional; sem critério = qualquer inscrito é elegível.
 */
public class CriterioCertificado {

    private final String id;
    private final String eventoId;
    private Integer percentualMinimo;
    private final List<String> atividadesObrigatorias;

    public CriterioCertificado(String id, String eventoId) {
        if (id == null || id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }
        if (eventoId == null || eventoId.isBlank()) {
            throw new DomainException("O evento do critério é obrigatório.");
        }
        this.eventoId = eventoId;
        this.atividadesObrigatorias = new ArrayList<>();
    }

    public CriterioCertificado(String eventoId) {
        this(null, eventoId);
    }

    public String getId() { return id; }
    public String getEventoId() { return eventoId; }
    public Integer getPercentualMinimo() { return percentualMinimo; }
    public List<String> getAtividadesObrigatorias() {
        return Collections.unmodifiableList(atividadesObrigatorias);
    }

    public void definirPercentualMinimo(Integer percentual) {
        if (percentual != null && (percentual < 0 || percentual > 100)) {
            throw new DomainException("Percentual mínimo deve estar entre 0 e 100.");
        }
        this.percentualMinimo = percentual;
    }

    public void exigirAtividade(String atividadeId) {
        if (atividadeId == null || atividadeId.isBlank()) {
            throw new DomainException("A atividade obrigatória é obrigatória.");
        }
        if (!atividadesObrigatorias.contains(atividadeId)) {
            atividadesObrigatorias.add(atividadeId);
        }
    }

    /**
     * RN-16: elegibilidade calculável a partir de inscrição + evento.
     * Retorna vazio = elegível; mensagem = justificativa (CA-08).
     */
    public java.util.Optional<String> verificar(Inscricao inscricao, Evento evento) {
        if (!inscricao.getEventoId().equals(eventoId)
                || !evento.getId().equals(eventoId)) {
            throw new DomainException("Critério não pertence a este evento/inscrição.");
        }
        List<Atividade> atividades = evento.getAtividades();
        if (atividades.isEmpty()) return java.util.Optional.of("Evento sem atividades.");
        long presentes = atividades.stream()
                .filter(a -> inscricao.getAtividadesSelecionadas().stream()
                        .anyMatch(s -> s.getId().equals(a.getId())))
                .filter(a -> inscricao.calcularPresenca(a) == StatusPresenca.PRESENTE)
                .count();
        int percentual = (int) Math.round(presentes * 100.0 / atividades.size());
        if (percentualMinimo != null && percentual < percentualMinimo) {
            return java.util.Optional.of("Presença de " + percentual
                    + "% abaixo do mínimo de " + percentualMinimo + "%.");
        }
        for (String obrigatoria : atividadesObrigatorias) {
            boolean ok = evento.getAtividades().stream()
                    .filter(a -> a.getId().equals(obrigatoria))
                    .findFirst()
                    .map(a -> inscricao.calcularPresenca(a) == StatusPresenca.PRESENTE)
                    .orElse(false);
            if (!ok) {
                return java.util.Optional.of(
                        "Atividade obrigatória pendente: " + obrigatoria + ".");
            }
        }
        return java.util.Optional.empty();
    }
}
