package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.ConfiguracaoOrganizacao;
import br.com.evento.domain.port.out.ConfiguracaoOrganizacaoRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §3 — parâmetros gerais da organização (admin).
 */
class GerenciarConfiguracoesUseCaseTest {

    private static class RepoEmMemoria implements ConfiguracaoOrganizacaoRepository {
        ConfiguracaoOrganizacao salva;

        @Override
        public Optional<ConfiguracaoOrganizacao> buscar() {
            return Optional.ofNullable(salva);
        }

        @Override
        public ConfiguracaoOrganizacao salvar(ConfiguracaoOrganizacao configuracao) {
            salva = configuracao;
            return configuracao;
        }
    }

    @Test
    void deveCriarPadraoQuandoNaoExiste() {
        RepoEmMemoria repo = new RepoEmMemoria();
        ConfiguracaoOrganizacao c = new GerenciarConfiguracoesUseCase(repo).obter();

        assertNotNull(c);
        assertNotNull(repo.salva);
        assertEquals(ConfiguracaoOrganizacao.ID_GLOBAL, c.getId());
        assertFalse(c.getNomeInstituicao().isBlank());
    }

    @Test
    void deveAtualizarParametrosGerais() {
        RepoEmMemoria repo = new RepoEmMemoria();
        GerenciarConfiguracoesUseCase uc = new GerenciarConfiguracoesUseCase(repo);

        ConfiguracaoOrganizacao c = uc.atualizar("Universidade Federal do Sul", "UFS",
                "contato@ufs.br", "https://ufs.br", 75, "admin-1");

        assertEquals("Universidade Federal do Sul", c.getNomeInstituicao());
        assertEquals("UFS", c.getSigla());
        assertEquals("contato@ufs.br", c.getEmailContato());
        assertEquals(75, c.getPercentualCertificadoPadrao());
        assertEquals("admin-1", c.getAtualizadoPor());
        assertNotNull(c.getAtualizadoEm());
    }

    @Test
    void deveRejeitarPercentualForaDaFaixa() {
        GerenciarConfiguracoesUseCase uc =
                new GerenciarConfiguracoesUseCase(new RepoEmMemoria());

        assertThrows(DomainException.class, () -> uc.atualizar(
                "Centro de Eventos Sul", null, null, null, 101, "admin-1"));
        assertThrows(DomainException.class, () -> uc.atualizar(
                "Centro de Eventos Sul", null, null, null, -1, "admin-1"));
    }

    @Test
    void deveRejeitarNomeCurtoEEmailInvalido() {
        GerenciarConfiguracoesUseCase uc =
                new GerenciarConfiguracoesUseCase(new RepoEmMemoria());

        assertThrows(DomainException.class, () -> uc.atualizar(
                "ab", null, null, null, null, "admin-1"));
        assertThrows(DomainException.class, () -> uc.atualizar(
                "Centro de Eventos Sul", null, "sem-arroba", null, null, "admin-1"));
    }

    @Test
    void deveLimparOpcionaisComVazio() {
        RepoEmMemoria repo = new RepoEmMemoria();
        GerenciarConfiguracoesUseCase uc = new GerenciarConfiguracoesUseCase(repo);
        uc.atualizar("Centro de Eventos Sul", "CES", "contato@x.br",
                "https://x.br", 80, "admin-1");

        ConfiguracaoOrganizacao c = uc.atualizar("Centro de Eventos Sul", "", "", "", null, "admin-1");

        assertNull(c.getSigla());
        assertNull(c.getEmailContato());
        assertNull(c.getSite());
        assertNull(c.getPercentualCertificadoPadrao());
    }
}
