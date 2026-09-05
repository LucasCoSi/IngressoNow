package br.com.evento.adapters.in.http;

import br.com.evento.application.usecase.GerarRelatorioPresencaUseCase;
import br.com.evento.application.usecase.RealizarCheckInQrCodeUseCase;
import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.port.in.QrCodeService;
import br.com.evento.domain.port.out.InscricaoRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

/**
 * /api/controle — check-in, QR e relatório CSV. Exige JWT.
 */
public class ControleHandler implements HttpHandler {

    private final RealizarCheckInQrCodeUseCase checkIn;
    private final GerarRelatorioPresencaUseCase relatorio;
    private final QrCodeService qrCodeService;
    private final InscricaoRepository inscricoes;
    private final JwtAuth jwt;

    public ControleHandler(RealizarCheckInQrCodeUseCase checkIn, GerarRelatorioPresencaUseCase relatorio,
                           QrCodeService qrCodeService, InscricaoRepository inscricoes, JwtAuth jwt) {
        this.checkIn = checkIn;
        this.relatorio = relatorio;
        this.qrCodeService = qrCodeService;
        this.inscricoes = inscricoes;
        this.jwt = jwt;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            if (!jwt.exigir(ex)) return;
            String metodo = ex.getRequestMethod().toUpperCase();
            String resto = ex.getRequestURI().getPath().substring("/api/controle".length());
            String[] partes = resto.substring(1).split("/");
            if (partes.length == 1 && "checkin".equals(partes[0]) && "POST".equals(metodo)) {
                var req = Json.ler(HttpErrors.corpo(ex), CheckInRequest.class);
                if (req.qrCodeToken() == null || req.qrCodeToken().isBlank()) {
                    throw new IllegalArgumentException("O token do QR Code é obrigatório");
                }
                checkIn.executar(req.qrCodeToken());
                HttpErrors.json(ex, 200, Map.of("ok", true));
                return;
            }
            if (partes.length == 3 && "qrcode".equals(partes[0]) && "GET".equals(metodo)) {
                Inscricao inscricao = inscricoes.buscarPorId(partes[1])
                        .orElseThrow(() -> new DomainException("Inscrição não encontrada."));
                HttpErrors.texto(ex, 200, qrCodeService.gerarTokenParaCheckIn(inscricao, partes[2]), "text/plain");
                return;
            }
            if (partes.length == 3 && "relatorio".equals(partes[0]) && "presenca".equals(partes[1])
                    && "GET".equals(metodo)) {
                String csv = relatorio.executarEmCsv(partes[2]);
                ex.getResponseHeaders().set("Content-Disposition",
                        "attachment; filename=\"presenca-" + partes[2] + ".csv\"");
                HttpErrors.texto(ex, 200, csv, "text/csv");
                return;
            }
            HttpErrors.json(ex, 404, StandardError.de(404, "Not Found",
                    "Rota não encontrada.", ex.getRequestURI().getPath()));
        } catch (Exception e) {
            HttpErrors.tratar(ex, e);
        }
    }

    public record CheckInRequest(String qrCodeToken) {
    }
}
