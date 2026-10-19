package br.com.evento.adapters.in.http;

import br.com.evento.application.usecase.GerarRelatorioInscritosUseCase;
import br.com.evento.application.usecase.GerarRelatorioPresencaUseCase;
import br.com.evento.application.usecase.RealizarCheckInManualUseCase;
import br.com.evento.application.usecase.RealizarCheckInQrCodeUseCase;
import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Administrador;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.port.in.QrCodeService;
import br.com.evento.domain.port.out.EventoRepository;
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
    private final RealizarCheckInManualUseCase checkInManual;
    private final GerarRelatorioPresencaUseCase relatorio;
    private final GerarRelatorioInscritosUseCase relatorioInscritos;
    private final QrCodeService qrCodeService;
    private final InscricaoRepository inscricoes;
    private final EventoRepository eventos;
    private final JwtAuth jwt;

    public ControleHandler(RealizarCheckInQrCodeUseCase checkIn, RealizarCheckInManualUseCase checkInManual,
                           GerarRelatorioPresencaUseCase relatorio,
                           GerarRelatorioInscritosUseCase relatorioInscritos,
                           QrCodeService qrCodeService, InscricaoRepository inscricoes,
                           EventoRepository eventos, JwtAuth jwt) {
        this.checkIn = checkIn;
        this.checkInManual = checkInManual;
        this.relatorio = relatorio;
        this.relatorioInscritos = relatorioInscritos;
        this.qrCodeService = qrCodeService;
        this.inscricoes = inscricoes;
        this.eventos = eventos;
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
                var logadoCheckin = jwt.autenticar(ex)
                        .orElseThrow(() -> new DomainException("Autenticação exigida."));
                var req = Json.ler(HttpErrors.corpo(ex), CheckInRequest.class);
                if (req.qrCodeToken() == null || req.qrCodeToken().isBlank()) {
                    throw new IllegalArgumentException("O token do QR Code é obrigatório");
                }
                // CA-04/RF-21: o participante elegível registra a própria presença;
                // o organizador dono do evento e o admin também podem lançar.
                var dados = qrCodeService.decodificarToken(req.qrCodeToken());
                Inscricao dona = inscricoes.buscarPorId(dados.inscricaoId())
                        .orElseThrow(() -> new DomainException("Inscrição não encontrada."));
                if (!exigirAcessoFrequencia(logadoCheckin, dona, ex)) return;
                checkIn.executar(req.qrCodeToken());
                HttpErrors.json(ex, 200, Map.of("ok", true));
                return;
            }
            // RF-22: lançamento manual com autoria (dono do evento ou admin)
            if (partes.length == 1 && "manual".equals(partes[0]) && "POST".equals(metodo)) {
                var usuario = jwt.autenticar(ex)
                        .orElseThrow(() -> new DomainException("Autenticação exigida."));
                if (!Papeis.exigirGestor(usuario, ex)) return;
                var req = Json.ler(HttpErrors.corpo(ex), CheckInManualRequest.class);
                if (req.inscricaoId() == null || req.inscricaoId().isBlank()) {
                    throw new IllegalArgumentException("A inscrição é obrigatória");
                }
                if (req.atividadeId() == null || req.atividadeId().isBlank()) {
                    throw new IllegalArgumentException("A atividade é obrigatória");
                }
                Inscricao alvo = inscricoes.buscarPorId(req.inscricaoId())
                        .orElseThrow(() -> new DomainException("Inscrição não encontrada."));
                // RF-22: manual é ato de gestão — participante não se auto-lança.
                Evento eventoAlvo = eventos.buscarPorId(alvo.getEventoId())
                        .orElseThrow(() -> new DomainException("Evento não encontrado."));
                if (!Papeis.exigirDonoEventoOuAdmin(usuario, eventoAlvo, ex)) return;
                checkInManual.executar(req.inscricaoId(), req.atividadeId(), usuario.getId());
                HttpErrors.json(ex, 200, Map.of("ok", true));
                return;
            }
            if (partes.length == 3 && "qrcode".equals(partes[0]) && "GET".equals(metodo)) {
                Inscricao inscricao = inscricoes.buscarPorId(partes[1])
                        .orElseThrow(() -> new DomainException("Inscrição não encontrada."));
                var logadoQr = jwt.autenticar(ex)
                        .orElseThrow(() -> new DomainException("Autenticação exigida."));
                if (!exigirAcessoFrequencia(logadoQr, inscricao, ex)) return;
                HttpErrors.texto(ex, 200, qrCodeService.gerarTokenParaCheckIn(inscricao, partes[2]), "text/plain");
                return;
            }
            if (partes.length == 3 && "relatorio".equals(partes[0]) && "presenca".equals(partes[1])
                    && "GET".equals(metodo)) {
                var logadoRel = jwt.autenticar(ex)
                        .orElseThrow(() -> new DomainException("Autenticação exigida."));
                if (!Papeis.exigirGestor(logadoRel, ex)) return;
                Evento eventoRel = eventos.buscarPorId(partes[2])
                        .orElseThrow(() -> new DomainException("Evento não encontrado."));
                if (!Papeis.exigirDonoEventoOuAdmin(logadoRel, eventoRel, ex)) return;
                String csv = relatorio.executarEmCsv(partes[2]);
                ex.getResponseHeaders().set("Content-Disposition",
                        "attachment; filename=\"presenca-" + partes[2] + ".csv\"");
                HttpErrors.texto(ex, 200, csv, "text/csv");
                return;
            }
            // RF-29: relatório de inscritos por evento (CSV)
            if (partes.length == 3 && "relatorio".equals(partes[0]) && "inscritos".equals(partes[1])
                    && "GET".equals(metodo)) {
                var logadoRel2 = jwt.autenticar(ex)
                        .orElseThrow(() -> new DomainException("Autenticação exigida."));
                if (!Papeis.exigirGestor(logadoRel2, ex)) return;
                Evento eventoRel2 = eventos.buscarPorId(partes[2])
                        .orElseThrow(() -> new DomainException("Evento não encontrado."));
                if (!Papeis.exigirDonoEventoOuAdmin(logadoRel2, eventoRel2, ex)) return;
                String csv = relatorioInscritos.executarEmCsv(partes[2]);
                ex.getResponseHeaders().set("Content-Disposition",
                        "attachment; filename=\"inscritos-" + partes[2] + ".csv\"");
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

    public record CheckInManualRequest(String inscricaoId, String atividadeId) {
    }

    /**
     * Frequência da inscrição (§3/RN-18): o próprio participante (CA-04),
     * o admin, ou o organizador dono do evento. Organizador alheio → 403.
     */
    private boolean exigirAcessoFrequencia(Usuario logado, Inscricao insc, HttpExchange ex)
            throws IOException {
        if (logado instanceof Administrador) return true;
        if (logado.getId().equals(insc.getParticipanteId())) return true;
        if (logado instanceof Organizador) {
            var evento = eventos.buscarPorId(insc.getEventoId());
            if (evento.isPresent()
                    && Papeis.exigirDonoEventoOuAdmin(logado, evento.get(), ex)) return true;
            return false;
        }
        HttpErrors.json(ex, 403, StandardError.de(403, "Forbidden",
                "Você só pode acessar os próprios recursos.", ex.getRequestURI().getPath()));
        return false;
    }
}
