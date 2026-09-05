package br.com.evento.adapters.in.http;

import br.com.evento.application.usecase.AutenticarUsuarioUseCase;
import br.com.evento.application.usecase.CadastrarUsuarioUseCase;
import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Usuario;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

/**
 * POST /api/auth/login e POST /api/auth/cadastro (rotas públicas).
 */
public class AuthHandler implements HttpHandler {

    private final AutenticarUsuarioUseCase autenticar;
    private final CadastrarUsuarioUseCase cadastrar;
    private final JwtAuth jwt;

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
            if (path.endsWith("/login")) {
                var req = Json.ler(HttpErrors.corpo(ex), LoginRequest.class);
                exigir(req.email(), "O e-mail é obrigatório");
                exigir(req.senha(), "A senha é obrigatória");
                try {
                    Usuario usuario = autenticar.executar(req.email(), req.senha());
                    HttpErrors.json(ex, 200, resposta(usuario));
                } catch (DomainException e) {
                    HttpErrors.json(ex, 401, Map.of("erro", e.getMessage()));
                }
                return;
            }
            if (path.endsWith("/cadastro")) {
                var req = Json.ler(HttpErrors.corpo(ex), CadastroRequest.class);
                exigir(req.nome(), "O nome é obrigatório");
                exigir(req.email(), "O e-mail é obrigatório");
                exigir(req.senha(), "A senha é obrigatória");
                exigir(req.tipo(), "O tipo é obrigatório");
                try {
                    Usuario usuario = cadastrar.executar(req.nome(), req.email(), req.senha(), req.tipo());
                    HttpErrors.json(ex, 201, resposta(usuario));
                } catch (DomainException e) {
                    HttpErrors.json(ex, 400, Map.of("erro", e.getMessage()));
                }
                return;
            }
            HttpErrors.json(ex, 404, StandardError.de(404, "Not Found", "Rota não encontrada.", path));
        } catch (Exception e) {
            HttpErrors.tratar(ex, e);
        }
    }

    private Map<String, String> resposta(Usuario usuario) {
        return Map.of(
                "token", jwt.emitir(usuario),
                "usuarioId", usuario.getId(),
                "nome", usuario.getNome(),
                "email", usuario.getEmail().getEndereco());
    }

    private static void exigir(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensagem);
    }

    public record LoginRequest(String email, String senha) {
    }

    public record CadastroRequest(String nome, String email, String senha, String tipo) {
    }
}
