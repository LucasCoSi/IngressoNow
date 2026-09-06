package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Evento {
    private final String id;
    private String titulo;
    private String descricao;
    private StatusEvento status;
    private final List<Atividade> atividades;
    private final Organizador organizador;
    private Integer maxVagas;
    /** RF-04: período do evento (início/fim geral, opcional no rascunho). */
    private br.com.evento.domain.model.vo.Periodo periodo;
    /** RF-04: local/modalidade (ex: "Centro de Convenções Sul", "Híbrido"). */
    private String local;
    /** RN-20: fuso do evento (ex: "America/Sao_Paulo"); default UTC. */
    private String fuso;
    /** RF-13: exige escolha de atividades na inscrição. */
    private boolean exigeEscolhaAtividades;
    /** RF-13: vagas controladas por atividade (além do limite do evento). */
    private boolean vagasPorAtividade;

    public Evento(String id, String titulo, String descricao, Organizador organizador) {
        if (id == null || id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }

        if (titulo == null || titulo.trim().isEmpty()) {
            throw new DomainException("O título do evento é obrigatório.");
        }
        if (organizador == null) {
            throw new DomainException("O organizador do evento é obrigatório.");
        }

        this.titulo = titulo;
        this.descricao = descricao;
        this.organizador = organizador;
        this.status = StatusEvento.RASCUNHO;
        this.atividades = new ArrayList<>();
        this.maxVagas = null;
        this.periodo = null;
        this.local = null;
        this.fuso = "UTC";
        this.exigeEscolhaAtividades = false;
        this.vagasPorAtividade = false;
    }

    public Evento(String titulo, String descricao, Organizador organizador) {
        this(null, titulo, descricao, organizador);
    }

    public String getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public StatusEvento getStatus() {
        return status;
    }

    public Organizador getOrganizador() {
        return organizador;
    }

    public Integer getMaxVagas() {
        return maxVagas;
    }

    /**
     * Configura o limite de vagas do evento.
     * null = sem limite (PoliticaSemLimite).
     */
    public void configurarLimiteDeVagas(Integer maxVagas) {
        if (maxVagas != null && maxVagas <= 0) {
            throw new DomainException("O limite de vagas deve ser positivo.");
        }
        this.maxVagas = maxVagas;
    }

    /**
     * Retorna a estratégia de inscrição derivada do maxVagas (Strategy Pattern).
     */
    public PoliticaInscricao getPoliticaInscricao() {
        if (maxVagas == null) {
            return new PoliticaSemLimite();
        }
        return new PoliticaComLimiteDeVagas(maxVagas);
    }

    public List<Atividade> getAtividades() {
        return Collections.unmodifiableList(atividades);
    }

    public void atualizar(String titulo, String descricao) {
        if (this.status != StatusEvento.RASCUNHO) {
            throw new DomainException("Apenas eventos em rascunho podem ser editados.");
        }
        if (titulo != null && !titulo.trim().isEmpty()) {
            this.titulo = titulo;
        }
        if (descricao != null) {
            this.descricao = descricao;
        }
    }

    /** RF-04: define período/local/modalidade (só em rascunho). */
    public void definirDetalhes(br.com.evento.domain.model.vo.Periodo periodo, String local) {
        if (this.status != StatusEvento.RASCUNHO) {
            throw new DomainException("Apenas eventos em rascunho podem ser editados.");
        }
        this.periodo = periodo;
        if (local != null && !local.isBlank()) this.local = local.trim();
    }

    /** RN-20: fuso IANA (ex: "America/Sao_Paulo"); inválido rejeita. */
    public void definirFuso(String fuso) {
        if (fuso == null || fuso.isBlank()) return;
        try {
            java.time.ZoneId.of(fuso.trim());
        } catch (Exception e) {
            throw new DomainException("Fuso horário inválido (use IANA, ex: America/Sao_Paulo).");
        }
        this.fuso = fuso.trim();
    }

    /** RF-13: regra de inscrição configurável. */
    public void configurarInscricao(boolean exigeEscolhaAtividades, boolean vagasPorAtividade) {
        if (this.status != StatusEvento.RASCUNHO) {
            throw new DomainException("Apenas eventos em rascunho podem ser editados.");
        }
        this.exigeEscolhaAtividades = exigeEscolhaAtividades;
        this.vagasPorAtividade = vagasPorAtividade;
    }

    public br.com.evento.domain.model.vo.Periodo getPeriodo() { return periodo; }
    public String getLocal() { return local; }
    public String getFuso() { return fuso; }
    public boolean isExigeEscolhaAtividades() { return exigeEscolhaAtividades; }
    public boolean isVagasPorAtividade() { return vagasPorAtividade; }

    /** Restaura flags de persistência (uso exclusivo dos adapters). */
    public void restaurarDetalhes(br.com.evento.domain.model.vo.Periodo periodo, String local,
                                  String fuso, boolean exigeEscolha, boolean vagasPorAtividade) {
        this.periodo = periodo;
        this.local = local;
        if (fuso != null && !fuso.isBlank()) this.fuso = fuso;
        this.exigeEscolhaAtividades = exigeEscolha;
        this.vagasPorAtividade = vagasPorAtividade;
    }

    public void publicar() {
        if (this.status != StatusEvento.RASCUNHO) {
            throw new DomainException("Apenas eventos em rascunho podem ser publicados.");
        }
        if (this.atividades.isEmpty()) {
            throw new DomainException("O evento precisa ter pelo menos uma atividade para ser publicado.");
        }
        this.status = StatusEvento.PUBLICADO;
    }

    public void abrirInscricoes() {
        if (this.status != StatusEvento.PUBLICADO) {
            throw new DomainException("Inscrições só podem ser abertas para eventos publicados.");
        }
        this.status = StatusEvento.INSCRICOES_ABERTAS;
    }

    public void encerrar() {
        if (this.status == StatusEvento.ENCERRADO) {
            throw new DomainException("O evento já está encerrado.");
        }
        this.status = StatusEvento.ENCERRADO;
    }

    /**
     * Reconstituição a partir da persistência — uso exclusivo dos adapters.
     * Não valida transições (o banco guarda um estado já válido).
     */
    public void restaurarEstado(StatusEvento status) {
        if (status == null) {
            throw new DomainException("O estado do evento é obrigatório.");
        }
        this.status = status;
    }

    public void adicionarAtividade(Atividade novaAtividade) {
        if (novaAtividade == null) {
            throw new DomainException("Atividade não pode ser nula.");
        }
        exigirSemConflito(novaAtividade, null);
        this.atividades.add(novaAtividade);
    }

    /**
     * RF-07: remarcação revalidada contra as irmãs (mesmo local + período
     * sobreposto). Decisão documentada: conflito de horário em locais
     * diferentes é permitido (grade paralela); só local igual bloqueia.
     */
    public void remarcarAtividade(String atividadeId, br.com.evento.domain.model.vo.Periodo novoPeriodo,
                                  String novoLocal) {
        Atividade alvo = atividades.stream()
                .filter(a -> a.getId().equals(atividadeId))
                .findFirst()
                .orElseThrow(() -> new DomainException("Atividade não encontrada no evento."));
        // Valida antes de mutar: simula a atividade remarcada e checa contra as irmãs
        Atividade simulada = new Atividade(alvo.getId(), alvo.getTitulo(), novoPeriodo, novoLocal,
                alvo.getTipo(), alvo.getTrilha());
        exigirSemConflito(simulada, atividadeId);
        alvo.remarcar(novoPeriodo, novoLocal);
    }

    private void exigirSemConflito(Atividade candidata, String ignorarId) {
        for (Atividade existente : atividades) {
            if (ignorarId != null && existente.getId().equals(ignorarId)) continue;
            if (existente.conflitaCom(candidata)
                    && existente.getLocal().equals(candidata.getLocal())) {
                throw new DomainException("Conflito de horário e local com outra atividade do evento.");
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Evento evento = (Evento) o;
        return Objects.equals(id, evento.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
