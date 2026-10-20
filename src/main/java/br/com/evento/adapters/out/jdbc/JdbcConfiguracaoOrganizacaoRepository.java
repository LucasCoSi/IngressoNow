package br.com.evento.adapters.out.jdbc;

import br.com.evento.domain.model.entity.ConfiguracaoOrganizacao;
import br.com.evento.domain.port.out.ConfiguracaoOrganizacaoRepository;
import br.com.evento.infra.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * §3 — adapter JDBC dos parâmetros gerais da organização (linha única).
 */
public class JdbcConfiguracaoOrganizacaoRepository implements ConfiguracaoOrganizacaoRepository {

    private final ConnectionFactory factory;

    public JdbcConfiguracaoOrganizacaoRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public Optional<ConfiguracaoOrganizacao> buscar() {
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM configuracoes_organizacao WHERE id = ?")) {
            ps.setString(1, ConfiguracaoOrganizacao.ID_GLOBAL);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapear(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar configurações da organização", e);
        }
    }

    @Override
    public ConfiguracaoOrganizacao salvar(ConfiguracaoOrganizacao configuracao) {
        LocalDateTime atualizadoEm = configuracao.getAtualizadoEm() == null
                ? LocalDateTime.now() : configuracao.getAtualizadoEm();
        try (Connection c = factory.open();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO configuracoes_organizacao"
                             + " (id, nome_instituicao, sigla, email_contato, site,"
                             + " percentual_certificado_padrao, atualizado_em, atualizado_por)"
                             + " VALUES (?,?,?,?,?,?,?,?)"
                             + " ON CONFLICT (id) DO UPDATE SET"
                             + " nome_instituicao=EXCLUDED.nome_instituicao,"
                             + " sigla=EXCLUDED.sigla,"
                             + " email_contato=EXCLUDED.email_contato,"
                             + " site=EXCLUDED.site,"
                             + " percentual_certificado_padrao=EXCLUDED.percentual_certificado_padrao,"
                             + " atualizado_em=EXCLUDED.atualizado_em,"
                             + " atualizado_por=EXCLUDED.atualizado_por")) {
            ps.setString(1, configuracao.getId());
            ps.setString(2, configuracao.getNomeInstituicao());
            ps.setString(3, configuracao.getSigla());
            ps.setString(4, configuracao.getEmailContato());
            ps.setString(5, configuracao.getSite());
            if (configuracao.getPercentualCertificadoPadrao() == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, configuracao.getPercentualCertificadoPadrao());
            }
            ps.setTimestamp(7, Timestamp.valueOf(atualizadoEm));
            ps.setString(8, configuracao.getAtualizadoPor());
            ps.executeUpdate();
            configuracao.registrarAtualizacao(atualizadoEm, configuracao.getAtualizadoPor());
            return configuracao;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao salvar configurações da organização", e);
        }
    }

    private static ConfiguracaoOrganizacao mapear(ResultSet rs) throws Exception {
        Object percentual = rs.getObject("percentual_certificado_padrao");
        Timestamp atualizadoEm = rs.getTimestamp("atualizado_em");
        return new ConfiguracaoOrganizacao(
                rs.getString("id"),
                rs.getString("nome_instituicao"),
                rs.getString("sigla"),
                rs.getString("email_contato"),
                rs.getString("site"),
                percentual == null ? null : ((Number) percentual).intValue(),
                atualizadoEm == null ? null : atualizadoEm.toLocalDateTime(),
                rs.getString("atualizado_por"));
    }
}
