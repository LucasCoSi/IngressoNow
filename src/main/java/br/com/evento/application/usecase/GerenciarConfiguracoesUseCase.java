package br.com.evento.application.usecase;

import br.com.evento.domain.model.entity.ConfiguracaoOrganizacao;
import br.com.evento.domain.port.out.ConfiguracaoOrganizacaoRepository;

import java.time.LocalDateTime;

/**
 * §3 — parâmetros gerais da organização.
 * Leitura para gestores (organizador vê a identidade); escrita só do
 * administrador (checada no handler com {@code Papeis.exigirAdministrador}).
 */
public class GerenciarConfiguracoesUseCase {

    private final ConfiguracaoOrganizacaoRepository repository;

    public GerenciarConfiguracoesUseCase(ConfiguracaoOrganizacaoRepository repository) {
        this.repository = repository;
    }

    /** Retorna a configuração vigente; cria o padrão se a linha não existir. */
    public ConfiguracaoOrganizacao obter() {
        return repository.buscar().orElseGet(() -> repository.salvar(
                new ConfiguracaoOrganizacao("Centro de Eventos Sul", "CES",
                        "contato@centroeventos.com.br", null, null)));
    }

    public ConfiguracaoOrganizacao atualizar(String nomeInstituicao, String sigla,
                                             String emailContato, String site,
                                             Integer percentualCertificadoPadrao,
                                             String autorId) {
        ConfiguracaoOrganizacao atual = obter();
        atual.atualizar(nomeInstituicao, sigla, emailContato, site,
                percentualCertificadoPadrao);
        atual.registrarAtualizacao(LocalDateTime.now(), autorId);
        return repository.salvar(atual);
    }
}
