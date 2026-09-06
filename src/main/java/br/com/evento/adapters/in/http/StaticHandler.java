package br.com.evento.adapters.in.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Serve o site estático do classpath (static/) — substitui o
 * resource handler do Spring Boot. Rotas amigáveis (/login, /eventos...)
 * + arquivos (/js/*.js, /css/*.css, páginas do System Design).
 */
public class StaticHandler implements HttpHandler {

    private static final Map<String, String> ROTAS = Map.ofEntries(
            Map.entry("/", "System Desgin v2__19-08-26/ingressonow-evento-publico.html"),
            Map.entry("/login", "System Desgin v2__19-08-26/ingressonow-login.html"),
            Map.entry("/cadastro", "System Desgin v2__19-08-26/ingressonow-cadastro.html"),
            Map.entry("/eventos", "System Desgin v2__19-08-26/ingressonow-eventos.html"),
            Map.entry("/evento-publico", "System Desgin v2__19-08-26/ingressonow-evento-publico.html"),
            Map.entry("/programacao", "System Desgin v2__19-08-26/ingressonow-programacao.html"),
            Map.entry("/inscricoes", "System Desgin v2__19-08-26/ingressonow-inscricoes.html"),
            Map.entry("/frequencia", "System Desgin v2__19-08-26/ingressonow-frequencia.html"),
            Map.entry("/avaliacoes", "System Desgin v2__19-08-26/ingressonow-avaliacoes.html"),
            Map.entry("/atividades", "System Desgin v2__19-08-26/ingressonow-atividades.html"),
            Map.entry("/relatorios", "System Desgin v2__19-08-26/ingressonow-relatorios.html"),
            Map.entry("/usuarios", "System Desgin v2__19-08-26/ingressonow-usuarios.html"),
            Map.entry("/configuracoes", "System Desgin v2__19-08-26/ingressonow-configuracoes.html"),
            Map.entry("/dashboard", "System Desgin v2__19-08-26/ingressonow-interface.html"),
            Map.entry("/area-participante", "System Desgin v2__19-08-26/ingressonow-area-participante.html"));

    private static final Map<String, String> MIME = Map.of(
            "html", "text/html", "js", "application/javascript",
            "css", "text/css", "json", "application/json",
            "png", "image/png", "jpg", "image/jpeg", "svg", "image/svg+xml",
            "ico", "image/x-icon");

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                HttpErrors.json(ex, 405, StandardError.de(405, "Method Not Allowed",
                        "Método não permitido.", ex.getRequestURI().getPath()));
                return;
            }
            String path = ex.getRequestURI().getPath();
            String recurso = ROTAS.get(path);
            if (recurso == null) {
                // Arquivo direto: /js/x.js, /css/y.css, /System Desgin.../z.html
                recurso = path.startsWith("/") ? path.substring(1) : path;
            }
            // Segurança: bloqueia .. e barra invertida
            if (recurso.contains("..") || recurso.contains("\\")) {
                HttpErrors.json(ex, 403, StandardError.de(403, "Forbidden",
                        "Acesso negado.", path));
                return;
            }
            String nome = "static/" + recurso;
            byte[] bytes;
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(nome)) {
                if (in == null) {
                    HttpErrors.json(ex, 404, StandardError.de(404, "Not Found",
                            "Página não encontrada.", path));
                    return;
                }
                bytes = in.readAllBytes();
            }
            String ext = "";
            int ponto = recurso.lastIndexOf('.');
            if (ponto >= 0) ext = recurso.substring(ponto + 1).toLowerCase();
            ex.getResponseHeaders().set("Content-Type",
                    MIME.getOrDefault(ext, "application/octet-stream") + "; charset=utf-8");
            // Evita flash de conteúdo antigo: sem cache de HTML/JS/CSS
            ex.getResponseHeaders().set("Cache-Control", "no-store, must-revalidate");
            ex.getResponseHeaders().set("Pragma", "no-cache");
            ex.sendResponseHeaders(200, bytes.length);
            try (var out = ex.getResponseBody()) {
                out.write(bytes);
            }
        } catch (Exception e) {
            HttpErrors.tratar(ex, e);
        }
    }
}
