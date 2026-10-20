package br.com.evento.adapters.in.http;

import br.com.evento.application.usecase.GerenciarConfiguracoesUseCase;
import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.ConfiguracaoOrganizacao;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * §3 — parâmetros gerais da organização (identidade + percentual padrão).
 * Rotas (JWT):
 * GET /api/configuracoes — organizador e administrador (leitura);
 * PUT /api/configuracoes — somente administrador.
 */
public class ConfiguracaoHandler implements HttpHandler {

    private final GerenciarConfiguracoesUseCase configuracoes;
    private final JwtAuth jwt;

    public ConfiguracaoHandler(GerenciarConfiguracoesUseCase configuracoes, JwtAuth jwt) {
        this.configuracoes = configuracoes;
        this.jwt = jwt;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            if (!jwt.exigir(ex)) return;
            var logado = jwt.autenticar(ex)
                    .orElseThrow(() -> new DomainException("Autenticação exigida."));
            String metodo = ex.getRequestMethod().toUpperCase();

            if ("GET".equals(metodo)) {
                if (!Papeis.exigirGestor(logado, ex)) return;
                HttpErrors.json(ex, 200, ConfiguracaoDTO.from(configuracoes.obter()));
                return;
            }
            if ("PUT".equals(metodo)) {
                if (!Papeis.exigirAdministrador(logado, ex)) return;
                var req = Json.ler(HttpErrors.corpo(ex), AtualizarConfiguracaoRequest.class);
                ConfiguracaoOrganizacao salvo = configuracoes.atualizar(
                        req.nomeInstituicao(), req.sigla(), req.emailContato(),
                        req.site(), req.percentualCertificadoPadrao(), logado.getId());
                HttpErrors.json(ex, 200, ConfiguracaoDTO.from(salvo));
                return;
            }
            HttpErrors.json(ex, 405, StandardError.de(405, "Method Not Allowed",
                    "Método não permitido.", ex.getRequestURI().getPath()));
        } catch (Exception e) {
            HttpErrors.tratar(ex, e);
        }
    }

    public record AtualizarConfiguracaoRequest(String nomeInstituicao, String sigla,
                                               String emailContato, String site,
                                               Integer percentualCertificadoPadrao) {
    }

    public record ConfiguracaoDTO(String nomeInstituicao, String sigla, String emailContato,
                                  String site, Integer percentualCertificadoPadrao,
                                  String atualizadoEm, String atualizadoPor) {
        static ConfiguracaoDTO from(ConfiguracaoOrganizacao c) {
            return new ConfiguracaoDTO(
                    c.getNomeInstituicao(), c.getSigla(), c.getEmailContato(), c.getSite(),
                    c.getPercentualCertificadoPadrao(),
                    c.getAtualizadoEm() == null ? null : c.getAtualizadoEm().toString(),
                    c.getAtualizadoPor());
        }
    }
}
