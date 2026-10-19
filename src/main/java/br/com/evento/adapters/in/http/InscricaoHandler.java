package br.com.evento.adapters.in.http;

import br.com.evento.application.usecase.CancelarInscricaoUseCase;
import br.com.evento.application.usecase.ConsultarAgendaUseCase;
import br.com.evento.application.usecase.InscreverParticipanteUseCase;
import br.com.evento.application.usecase.SelecionarAtividadeUseCase;
import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Administrador;
import br.com.evento.domain.model.entity.AgendaDoParticipante;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.UsuarioRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /api/inscricoes — todas as rotas exigem JWT.
 * Mesmos contratos do antigo InscricaoRestController.
 */
public class InscricaoHandler implements HttpHandler {

    private final InscreverParticipanteUseCase inscrever;
    private final ConsultarAgendaUseCase agenda;
    private final SelecionarAtividadeUseCase selecionar;
    private final CancelarInscricaoUseCase cancelar;
    private final InscricaoRepository inscricoes;
    private final UsuarioRepository usuarios;
    private final EventoRepository eventos;
    private final JwtAuth jwt;

    public InscricaoHandler(InscreverParticipanteUseCase inscrever, ConsultarAgendaUseCase agenda,
                            SelecionarAtividadeUseCase selecionar, CancelarInscricaoUseCase cancelar,
                            InscricaoRepository inscricoes, UsuarioRepository usuarios,
                            EventoRepository eventos, JwtAuth jwt) {
        this.inscrever = inscrever;
        this.agenda = agenda;
        this.selecionar = selecionar;
        this.cancelar = cancelar;
        this.inscricoes = inscricoes;
        this.usuarios = usuarios;
        this.eventos = eventos;
        this.jwt = jwt;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            if (!jwt.exigir(ex)) return;
            var logado = jwt.autenticar(ex)
                    .orElseThrow(() -> new DomainException("Autenticação exigida."));
            String metodo = ex.getRequestMethod().toUpperCase();
            String resto = ex.getRequestURI().getPath().substring("/api/inscricoes".length());
            String query = ex.getRequestURI().getRawQuery();
            if (resto.isEmpty() || resto.equals("/")) {
                if ("GET".equals(metodo)) {
                    Map<String, String> params = query(query);
                    // RNF-10: projeção leve (1 query com JOINs) em vez do
                    // agregado completo por linha.
                    if (params.containsKey("participanteId") && !params.get("participanteId").isBlank()) {
                        // Próprias inscrições, admin, ou organizador via ?eventoId=.
                        if (!Papeis.exigirDonoOuAdmin(logado, params.get("participanteId"), ex)) return;
                        HttpErrors.json(ex, 200, inscricoes
                                .listarResumidoPorParticipante(params.get("participanteId")).stream()
                                .map(r -> new InscricaoDTO(r.id(), r.participanteId(), r.participanteNome(),
                                        r.eventoId(), r.eventoTitulo(), r.dataInscricao().toString()))
                                .toList());
                    } else {
                        // Inscrições do evento: dono do evento ou admin (RN-18).
                        String eventoId = params.getOrDefault("eventoId", "");
                        Evento evento = eventos.buscarPorId(eventoId)
                                .orElseThrow(() -> new DomainException("Evento não encontrado."));
                        if (!Papeis.exigirDonoEventoOuAdmin(logado, evento, ex)) return;
                        HttpErrors.json(ex, 200, inscricoes
                                .listarResumidoPorEvento(params.getOrDefault("eventoId", "")).stream()
                                .map(r -> new InscricaoDTO(r.id(), r.participanteId(), r.participanteNome(),
                                        r.eventoId(), r.eventoTitulo(), r.dataInscricao().toString()))
                                .toList());
                    }
                } else if ("POST".equals(metodo)) {
                    var req = Json.ler(HttpErrors.corpo(ex), InscreverRequest.class);
                    if (req.participanteId() == null || req.participanteId().isBlank()) {
                        throw new IllegalArgumentException("O ID do participante é obrigatório");
                    }
                    if (req.eventoId() == null || req.eventoId().isBlank()) {
                        throw new IllegalArgumentException("O ID do evento é obrigatório");
                    }
                    // Inscrição manual: o próprio participante, admin, ou o
                    // organizador dono do evento (gestão de inscritos).
                    if (!logado.getId().equals(req.participanteId())
                            && !(logado instanceof Administrador)) {
                        Evento destino = eventos.buscarPorId(req.eventoId())
                                .orElseThrow(() -> new DomainException("Evento não encontrado."));
                        if (!Papeis.exigirDonoEventoOuAdmin(logado, destino, ex)) return;
                    }
                    HttpErrors.json(ex, 200, dto(inscrever.executar(req.participanteId(), req.eventoId())));
                } else {
                    HttpErrors.json(ex, 405, StandardError.de(405, "Method Not Allowed",
                            "Método não permitido.", ex.getRequestURI().getPath()));
                }
                return;
            }
            String[] partes = resto.substring(1).split("/");
            if (partes.length == 1 && "DELETE".equals(metodo)) {
                Inscricao alvo = inscricoes.buscarPorId(partes[0])
                        .orElseThrow(() -> new DomainException("Inscrição não encontrada."));
                if (!exigirAcessoInscricao(logado, alvo, ex)) return;
                cancelar.executar(partes[0]);
                HttpErrors.semConteudo(ex);
                return;
            }
            if (partes.length == 3 && "atividades".equals(partes[1]) && "POST".equals(metodo)) {
                Inscricao alvo = inscricoes.buscarPorId(partes[0])
                        .orElseThrow(() -> new DomainException("Inscrição não encontrada."));
                if (!exigirAcessoInscricao(logado, alvo, ex)) return;
                selecionar.executar(partes[0], partes[2]);
                HttpErrors.json(ex, 200, Map.of("ok", true));
                return;
            }
            if (partes.length == 2 && "agenda".equals(partes[1]) && "GET".equals(metodo)) {
                Inscricao alvoAgenda = inscricoes.buscarPorId(partes[0])
                        .orElseThrow(() -> new DomainException("Inscrição não encontrada."));
                if (!exigirAcessoInscricao(logado, alvoAgenda, ex)) return;
                AgendaDoParticipante a = agenda.executar(partes[0]);
                HttpErrors.json(ex, 200, new AgendaDTO(a.getParticipanteId(),
                        a.getAtividades().stream()
                                .map(at -> new AtividadeDTO(at.getId(), at.getTitulo(),
                                        at.getPeriodo().getInicio().toString(),
                                        at.getPeriodo().getFim().toString(), at.getLocal(),
                                        at.getTipo(), at.getTrilha(), at.getMaxVagas()))
                                .toList()));
                return;
            }
            HttpErrors.json(ex, 404, StandardError.de(404, "Not Found",
                    "Rota não encontrada.", ex.getRequestURI().getPath()));
        } catch (Exception e) {
            HttpErrors.tratar(ex, e);
        }
    }

    private InscricaoDTO dto(Inscricao i) {
        return dto(i, new HashMap<>(), new HashMap<>());
    }

    /**
     * Acesso a uma inscrição (§3/RN-18): o próprio participante, admin, ou o
     * organizador dono do evento. Qualquer outro organizador recebe 403.
     */
    private boolean exigirAcessoInscricao(Usuario logado, Inscricao insc, HttpExchange ex)
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

    private InscricaoDTO dto(Inscricao i, Map<String, String> nomes, Map<String, String> titulos) {
        String participanteNome = nomes.computeIfAbsent(i.getParticipanteId(), pid -> {
            try {
                return usuarios.buscarPorId(pid).map(u -> u.getNome()).orElse(pid);
            } catch (Exception e) {
                return pid;
            }
        });
        String eventoTitulo = titulos.computeIfAbsent(i.getEventoId(), eid -> {
            try {
                return eventos.buscarPorId(eid).map(e -> e.getTitulo()).orElse(eid);
            } catch (Exception e) {
                return eid;
            }
        });
        return new InscricaoDTO(i.getId(), i.getParticipanteId(), participanteNome,
                i.getEventoId(), eventoTitulo, i.getDataInscricao().toString());
    }

    static Map<String, String> query(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();
        Map<String, String> params = new java.util.HashMap<>();
        for (String par : raw.split("&")) {
            int idx = par.indexOf('=');
            if (idx > 0) {
                params.put(
                        java.net.URLDecoder.decode(par.substring(0, idx), java.nio.charset.StandardCharsets.UTF_8),
                        java.net.URLDecoder.decode(par.substring(idx + 1), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    public record InscreverRequest(String participanteId, String eventoId) {
    }

    public record InscricaoDTO(String id, String participanteId, String participanteNome,
                               String eventoId, String eventoTitulo, String dataInscricao) {
    }

    public record AtividadeDTO(String id, String titulo, String inicio, String fim, String local,
                               String tipo, String trilha, Integer maxVagas) {
    }

    public record AgendaDTO(String participanteId, List<AtividadeDTO> atividades) {
    }
}
