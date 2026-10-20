package br.com.evento.domain.port.out;

import br.com.evento.domain.model.entity.ConfiguracaoOrganizacao;

import java.util.Optional;

/**
 * Porta de saída dos parâmetros gerais da organização (§3).
 * Linha única ({@link ConfiguracaoOrganizacao#ID_GLOBAL}).
 */
public interface ConfiguracaoOrganizacaoRepository {

    Optional<ConfiguracaoOrganizacao> buscar();

    ConfiguracaoOrganizacao salvar(ConfiguracaoOrganizacao configuracao);
}
