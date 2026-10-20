package br.com.evento.domain.model.entity;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.vo.Email;

import java.time.LocalDateTime;

/**
 * §3 — parâmetros gerais da organização (competência do ADMINISTRADOR):
 * identidade da instituição + percentual padrão de certificado.
 *
 * <p>Linha única ({@link #ID_GLOBAL}); vale para todos os eventos. O
 * percentual padrão é usado como fallback quando o evento não tem critério
 * próprio (RF-32/RN-16). Textual em certificados e rodapés da gestão.</p>
 */
public class ConfiguracaoOrganizacao {

    public static final String ID_GLOBAL = "global";

    private final String id;
    private String nomeInstituicao;
    private String sigla;
    private String emailContato;
    private String site;
    private Integer percentualCertificadoPadrao;
    private LocalDateTime atualizadoEm;
    private String atualizadoPor;

    public ConfiguracaoOrganizacao(String nomeInstituicao, String sigla,
                                   String emailContato, String site,
                                   Integer percentualCertificadoPadrao) {
        this(ID_GLOBAL, nomeInstituicao, sigla, emailContato, site,
                percentualCertificadoPadrao, null, null);
    }

    public ConfiguracaoOrganizacao(String id, String nomeInstituicao, String sigla,
                                   String emailContato, String site,
                                   Integer percentualCertificadoPadrao,
                                   LocalDateTime atualizadoEm, String atualizadoPor) {
        this.id = id == null || id.isBlank() ? ID_GLOBAL : id.trim();
        atualizar(nomeInstituicao, sigla, emailContato, site, percentualCertificadoPadrao);
        this.atualizadoEm = atualizadoEm == null ? LocalDateTime.now() : atualizadoEm;
        this.atualizadoPor = atualizadoPor;
    }

    /**
     * Altera os parâmetros gerais com validação de domínio.
     * Nome é obrigatório; e-mail, site e percentual são opcionais.
     */
    public void atualizar(String nomeInstituicao, String sigla, String emailContato,
                          String site, Integer percentualCertificadoPadrao) {
        if (nomeInstituicao == null || nomeInstituicao.isBlank()
                || nomeInstituicao.trim().length() < 3) {
            throw new DomainException("O nome da instituição deve ter ao menos 3 caracteres.");
        }
        if (percentualCertificadoPadrao != null
                && (percentualCertificadoPadrao < 0 || percentualCertificadoPadrao > 100)) {
            throw new DomainException("O percentual padrão deve estar entre 0 e 100.");
        }
        this.nomeInstituicao = nomeInstituicao.trim();
        this.sigla = normalizar(sigla);
        this.emailContato = emailValido(emailContato);
        this.site = siteValido(site);
        this.percentualCertificadoPadrao = percentualCertificadoPadrao;
    }

    /** Registra autoria/data da última alteração (auditoria simples). */
    public void registrarAtualizacao(LocalDateTime quando, String autorId) {
        this.atualizadoEm = quando == null ? LocalDateTime.now() : quando;
        this.atualizadoPor = autorId;
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String emailValido(String valor) {
        if (valor == null || valor.isBlank()) return null;
        // Reaproveita o VO de e-mail (mesma regra do cadastro).
        return new Email(valor).getEndereco();
    }

    private static String siteValido(String valor) {
        String normalizado = normalizar(valor);
        if (normalizado == null) return null;
        if (!normalizado.matches("(?i)^https?://\\S+$")) {
            throw new DomainException("O site deve começar com http:// ou https://.");
        }
        return normalizado;
    }

    public String getId() { return id; }
    public String getNomeInstituicao() { return nomeInstituicao; }
    public String getSigla() { return sigla; }
    public String getEmailContato() { return emailContato; }
    public String getSite() { return site; }
    public Integer getPercentualCertificadoPadrao() { return percentualCertificadoPadrao; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public String getAtualizadoPor() { return atualizadoPor; }
}
