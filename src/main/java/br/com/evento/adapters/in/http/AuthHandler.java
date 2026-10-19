package br.com.evento.adapters.in.http;

import br.com.evento.application.usecase.AutenticarUsuarioUseCase;
import br.com.evento.application.usecase.CadastrarUsuarioUseCase;
import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Administrador;
import br.com.evento.domain.model.entity.Usuario;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

/**
 * POST /api/auth/login e POST /api/auth/cadastro (rotas públicas).
 *
 * <p>Proteções: mensagens de credencial genéricas (sem enumerar e-mails),
 * rate limit em memória (429 + Retry-After) e autorização de perfis de gestão
 * no núcleo ({@link CadastrarUsuarioUseCase}), não apenas na tela.</p>
 */
public class AuthHandler implements HttpHandler {

    private static final int LOGIN_MAX_TENTATIVAS = 10;
    private static final long LOGIN_JANELA_MS = 60_000;
    private static final int CADASTRO_MAX_TENTATIVAS = 5;
    private static final long CADASTRO_JANELA_MS = 10 * 60_000;

    private final AutenticarUsuarioUseCase autenticar;
    private final CadastrarUsuarioUseCase cadastrar;
    private final JwtAuth jwt;
    private final RateLimiter limite = new RateLimiter();

    public AuthHandler(AutenticarUsuarioUseCase autenticar, CadastrarUsuarioUseCase cadastrar, JwtAuth jwt) {
        this.autenticar = autenticar;
        this.cadastrar = cadastrar;
        this.jwt = jwt;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                HttpErrors.json(ex, 405, StandardError.de(405, "Method Not Allowed",
                        "Método não permitido.", path));
                return;
            }
            String ip = ipDoCliente(ex);
            if (path.endsWith("/login")) {
                String chave = "login:" + ip;
                if (!limite.permitir(chave, LOGIN_MAX_TENTATIVAS, LOGIN_JANELA_MS)) {
                    responder429(ex, path, limite.esperaSegundos(
                            chave, LOGIN_MAX_TENTATIVAS, LOGIN_JANELA_MS));
                    return;
                }
                var req = Json.ler(HttpErrors.corpo(ex), LoginRequest.class);
                exigir(req.email(), "O e-mail é obrigatório");
                exigir(req.senha(), "A senha é obrigatória");
                try {
                    Usuario usuario = autenticar.executar(req.email(), req.senha());
                    limite.limpar(chave);
                    HttpErrors.json(ex, 200, resposta(usuario));
                } catch (DomainException e) {
                    // Mensagem genérica: não revela se o e-mail existe.
                    HttpErrors.json(ex, 401, StandardError.de(401, "Unauthorized",
                            e.getMessage(), path));
                }
                return;
            }
            if (path.endsWith("/cadastro")) {
                // §3/RN-18: participante é público; organizador e administrador
                // só nascem de uma conta administradora.
                Usuario solicitante = jwt.autenticar(ex).orElse(null);
                // O rate limit protege o cadastro público; convites feitos por
                // um admin autenticado não são limitados (uso operacional).
                if (!(solicitante instanceof Administrador)) {
                    String chave = "cadastro:" + ip;
                    if (!limite.permitir(chave, CADASTRO_MAX_TENTATIVAS, CADASTRO_JANELA_MS)) {
                        responder429(ex, path, limite.esperaSegundos(
                                chave, CADASTRO_MAX_TENTATIVAS, CADASTRO_JANELA_MS));
                        return;
                    }
                }
                var req = Json.ler(HttpErrors.corpo(ex), CadastroRequest.class);
                exigir(req.nome(), "O nome é obrigatório");
                exigir(req.email(), "O e-mail é obrigatório");
                exigir(req.senha(), "A senha é obrigatória");
                exigir(req.tipo(), "O tipo é obrigatório");
                boolean perfilDeGestao = "ADMINISTRADOR".equalsIgnoreCase(req.tipo())
                        || "ORGANIZADOR".equalsIgnoreCase(req.tipo());
                if (perfilDeGestao && !(solicitante instanceof Administrador)) {
                    HttpErrors.json(ex, 403, StandardError.de(403, "Forbidden",
                            "Apenas administradores criam contas de organizador ou administrador.",
                            path));
                    return;
                }
                try {
                    Usuario usuario = cadastrar.executar(
                            req.nome(), req.email(), req.senha(), req.tipo(), solicitante);
                    HttpErrors.json(ex, 201, resposta(usuario));
                } catch (DomainException e) {
                    HttpErrors.json(ex, 400, StandardError.de(400, "Bad Request",
                            e.getMessage(), path));
                }
                return;
            }
            HttpErrors.json(ex, 404, StandardError.de(404, "Not Found", "Rota não encontrada.", path));
        } catch (Exception e) {
            HttpErrors.tratar(ex, e);
        }
    }

    private static void responder429(HttpExchange ex, String path, long segundos) throws IOException {
        ex.getResponseHeaders().set("Retry-After", String.valueOf(Math.max(1, segundos)));
        HttpErrors.json(ex, 429, StandardError.de(429, "Too Many Requests",
                "Muitas tentativas. Aguarde " + Math.max(1, segundos)
                        + " segundo(s) e tente novamente.", path));
    }

    private static String ipDoCliente(HttpExchange ex) {
        var endereco = ex.getRemoteAddress();
        if (endereco == null || endereco.getAddress() == null) return "desconhecido";
        return endereco.getAddress().getHostAddress();
    }

    private Map<String, String> resposta(Usuario usuario) {
        return Map.of(
                "token", jwt.emitir(usuario),
                "usuarioId", usuario.getId(),
                "nome", usuario.getNome(),
                "email", usuario.getEmail().getEndereco(),
                "tipo", usuario.getClass().getSimpleName().toUpperCase());
    }

    private static void exigir(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensagem);
    }

    public record LoginRequest(String email, String senha) {
    }

    public record CadastroRequest(String nome, String email, String senha, String tipo) {
    }
}
