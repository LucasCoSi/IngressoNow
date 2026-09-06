package br.com.evento.adapters.in.http;

import br.com.evento.application.usecase.GerenciarCertificadosUseCase;
import br.com.evento.domain.model.entity.Certificado;
import br.com.evento.domain.model.entity.CriterioCertificado;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

/**
 * RF-32–35 (D) + CA-08 — certificados.
 * Rotas (todas JWT; escrita = organizador):
 * POST /api/eventos/{id}/certificados/criterio
 * POST /api/eventos/{id}/certificados/participante (body {inscricaoId})
 * POST /api/eventos/{id}/certificados/palestrante (body {pessoaId})
 * GET  /api/eventos/{id}/certificados
 * GET  /api/certificados?participanteId= | ?codigo=
 */
public class CertificadoHandler implements HttpHandler {

    private final GerenciarCertificadosUseCase gerenciar;
    private final JwtAuth jwt;

    public CertificadoHandler(GerenciarCertificadosUseCase gerenciar, JwtAuth jwt) {
        this.gerenciar = gerenciar;
        this.jwt = jwt;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            if (!jwt.exigir(ex)) return;
            String metodo = ex.getRequestMethod().toUpperCase();
            String path = ex.getRequestURI().getPath();

            if (path.startsWith("/api/eventos/") && path.contains("/certificados")) {
                String eventoId = path.substring("/api/eventos/".length(),
                        path.indexOf("/certificados")).replace("/", "");
                String sufixo = path.substring(path.indexOf("/certificados")
                        + "/certificados".length());
                if ("/criterio".equals(sufixo) && "POST".equals(metodo)) {
                    var req = Json.ler(HttpErrors.corpo(ex), CriterioRequest.class);
                    CriterioCertificado c = gerenciar.definirCriterio(eventoId,
                            req.percentualMinimo(), req.obrigatorias());
                    HttpErrors.json(ex, 200, CriterioDTO.from(c));
                    return;
                }
                if ("/participante".equals(sufixo) && "POST".equals(metodo)) {
                    var req = Json.ler(HttpErrors.corpo(ex), EmitirRequest.class);
                    exigir(req.inscricaoId(), "A inscrição é obrigatória");
                    HttpErrors.json(ex, 201, CertificadoDTO.from(
                            gerenciar.emitirParaParticipante(eventoId, req.inscricaoId())));
                    return;
                }
                if ("/palestrante".equals(sufixo) && "POST".equals(metodo)) {
                    var req = Json.ler(HttpErrors.corpo(ex), EmitirPalestranteRequest.class);
                    exigir(req.pessoaId(), "A pessoa é obrigatória");
                    HttpErrors.json(ex, 201, CertificadoDTO.from(
                            gerenciar.emitirParaPalestrante(eventoId, req.pessoaId())));
                    return;
                }
                if ((sufixo.isEmpty() || sufixo.equals("/")) && "GET".equals(metodo)) {
                    HttpErrors.json(ex, 200, gerenciar.listarPorEvento(eventoId).stream()
                            .map(CertificadoDTO::from).toList());
                    return;
                }
            }
            if (path.equals("/api/certificados") && "GET".equals(metodo)) {
                var params = InscricaoHandler.query(ex.getRequestURI().getRawQuery());
                if (params.containsKey("codigo")) {
                    HttpErrors.json(ex, 200, gerenciar.listarPorEvento("")
                            .stream().filter(c -> c.getCodigo().equals(params.get("codigo")))
                            .map(CertificadoDTO::from).toList());
                    return;
                }
                String participanteId = params.getOrDefault("participanteId", "");
                if (participanteId.isBlank()) {
                    throw new IllegalArgumentException(
                            "Informe participanteId ou codigo.");
                }
                HttpErrors.json(ex, 200, gerenciar.listarPorParticipante(participanteId)
                        .stream().map(CertificadoDTO::from).toList());
                return;
            }
            HttpErrors.json(ex, 404, StandardError.de(404, "Not Found",
                    "Rota não encontrada.", path));
        } catch (Exception e) {
            HttpErrors.tratar(ex, e);
        }
    }

    private static void exigir(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensagem);
    }

    public record CriterioRequest(Integer percentualMinimo, List<String> obrigatorias) {
    }

    public record EmitirRequest(String inscricaoId) {
    }

    public record EmitirPalestranteRequest(String pessoaId) {
    }

    public record CriterioDTO(String id, String eventoId, Integer percentualMinimo,
                              List<String> obrigatorias) {
        static CriterioDTO from(CriterioCertificado c) {
            return new CriterioDTO(c.getId(), c.getEventoId(),
                    c.getPercentualMinimo(), c.getAtividadesObrigatorias());
        }
    }

    public record CertificadoDTO(String id, String codigo, String eventoId,
                                 String participanteId, String participanteNome,
                                 String papelDeclarado, int cargaHoraria, String emitidoEm) {
        static CertificadoDTO from(Certificado c) {
            return new CertificadoDTO(c.getId(), c.getCodigo(), c.getEventoId(),
                    c.getParticipanteId(), c.getParticipanteNome(),
                    c.getPapelDeclarado(), c.getCargaHoraria(),
                    c.getEmitidoEm().toString());
        }
    }
}
