package br.com.evento.adapters.in.http;

import br.com.evento.application.usecase.EditarUsuarioUseCase;
import br.com.evento.application.usecase.GerenciarUsuariosUseCase;
import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.port.out.UsuarioRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

/**
 * RF-03 — /api/usuarios/{id}: GET (próprio perfil) e PUT (nome/senha).
 * Admin (§3): GET /api/usuarios (listar, só ORGANIZADOR) e
 * DELETE /api/usuarios/{id} (só ORGANIZADOR, com travas do use case).
 * Exige JWT em tudo.
 */
public class UsuarioHandler implements HttpHandler {

    private final EditarUsuarioUseCase atualizar;
    private final GerenciarUsuariosUseCase gerenciar;
    private final UsuarioRepository usuarios;
    private final JwtAuth jwt;

    public UsuarioHandler(EditarUsuarioUseCase atualizar, GerenciarUsuariosUseCase gerenciar,
                          UsuarioRepository usuarios, JwtAuth jwt) {
        this.atualizar = atualizar;
        this.gerenciar = gerenciar;
        this.usuarios = usuarios;
        this.jwt = jwt;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            var logado = jwt.autenticar(ex);
            if (logado.isEmpty()) {
                HttpErrors.json(ex, 401, StandardError.de(401, "Unauthorized",
                        "Autenticação exigida.", ex.getRequestURI().getPath()));
                return;
            }
            String metodo = ex.getRequestMethod().toUpperCase();
            String resto = ex.getRequestURI().getPath().substring("/api/usuarios".length());
            // GET /api/usuarios — admin lista todos
            if ((resto.isEmpty() || resto.equals("/")) && "GET".equals(metodo)) {
                exigirOrganizador(logado.get(), ex);
                HttpErrors.json(ex, 200, gerenciar.listarTodos().stream()
                        .map(UsuarioHandler::perfil).toList());
                return;
            }
            String[] partes = resto.substring(1).split("/");
            if (partes.length != 1 || partes[0].isBlank()) {
                HttpErrors.json(ex, 404, StandardError.de(404, "Not Found",
                        "Rota não encontrada.", ex.getRequestURI().getPath()));
                return;
            }
            // DELETE /api/usuarios/{id} — admin remove (com travas)
            if ("DELETE".equals(metodo)) {
                exigirOrganizador(logado.get(), ex);
                gerenciar.remover(logado.get().getId(), partes[0]);
                HttpErrors.semConteudo(ex);
                return;
            }
            String id = partes[0];
            if (!logado.get().getId().equals(id)) {
                HttpErrors.json(ex, 403, StandardError.de(403, "Forbidden",
                        "Você só pode acessar o próprio perfil.", ex.getRequestURI().getPath()));
                return;
            }
            if ("GET".equals(metodo)) {
                Usuario u = usuarios.buscarPorId(id)
                        .orElseThrow(() -> new DomainException("Usuário não encontrado."));
                HttpErrors.json(ex, 200, perfil(u));
                return;
            }
            if ("PUT".equals(metodo)) {
                var req = Json.ler(HttpErrors.corpo(ex), AtualizarRequest.class);
                if ((req.nome() == null || req.nome().isBlank())
                        && (req.senha() == null || req.senha().isBlank())) {
                    throw new IllegalArgumentException("Informe ao menos nome ou senha para atualizar.");
                }
                Usuario u = atualizar.executar(id, req.nome(), req.senha());
                HttpErrors.json(ex, 200, perfil(u));
                return;
            }
            HttpErrors.json(ex, 405, StandardError.de(405, "Method Not Allowed",
                    "Método não permitido.", ex.getRequestURI().getPath()));
        } catch (AcessoNegado ignored) {
            // Resposta 403 já enviada por exigirOrganizador
        } catch (Exception e) {
            HttpErrors.tratar(ex, e);
        }
    }

    private static void exigirOrganizador(Usuario logado, HttpExchange ex) throws IOException {
        if (!(logado instanceof Organizador)) {
            HttpErrors.json(ex, 403, StandardError.de(403, "Forbidden",
                    "Apenas organizadores.", ex.getRequestURI().getPath()));
            throw new AcessoNegado();
        }
    }

    /** Controle de fluxo interno: exigirOrganizador já respondeu 403. */
    private static final class AcessoNegado extends RuntimeException {
    }

    private static Map<String, String> perfil(Usuario u) {
        return Map.of(
                "id", u.getId(),
                "nome", u.getNome(),
                "email", u.getEmail().getEndereco(),
                "tipo", u.getClass().getSimpleName().toUpperCase());
    }

    public record AtualizarRequest(String nome, String senha) {
    }
}
