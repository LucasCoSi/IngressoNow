package br.com.evento.adapters.in.http;

import br.com.evento.infra.FabricaAplicacao;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP em Java SE puro (com.sun.net.httpserver, parte do JDK).
 * Expõe os mesmos contratos /api/* do backend anterior.
 */
public class ServidorApi {

    private final FabricaAplicacao app;
    private HttpServer server;

    public ServidorApi(FabricaAplicacao app) {
        this.app = app;
    }

    public void iniciar(int porta) throws IOException {
        JwtAuth jwt = new JwtAuth(app.config.jwtSecret(), app.config.jwtExpirationMs(), app.usuarios);
        EventoHandler eventoHandler = new EventoHandler(app.listarEventosPublicados, app.cadastrarEvento,
                app.adicionarAtividade, app.remarcarAtividade, app.vincularPessoa,
                app.eventos, app.inscricoes, app.pessoas, jwt);
        AvaliacaoHandler avaliacaoHandler = new AvaliacaoHandler(app.criarQuestionario,
                app.submeterAvaliacao, app.consolidarAvaliacoes, app.questionarios, jwt);
        CertificadoHandler certificadoHandler =
                new CertificadoHandler(app.gerenciarCertificados, jwt);
        server = HttpServer.create(new InetSocketAddress(porta), 0);
        server.createContext("/api/auth", envolver(new AuthHandler(app.autenticarUsuario, app.cadastrarUsuario, jwt)));
        server.createContext("/api/eventos", envolver(exchange -> {
            // Prefixos maiores delegam: questionários e certificados
            String path = exchange.getRequestURI().getPath();
            if (path.matches("(?i).*/questionarios/?$")) {
                avaliacaoHandler.handle(exchange);
            } else if (path.matches("(?i).*/certificados(/.*)?$")) {
                certificadoHandler.handle(exchange);
            } else {
                eventoHandler.handle(exchange);
            }
        }));
        server.createContext("/api/inscricoes", envolver(new InscricaoHandler(app.inscreverParticipante,
                app.consultarAgenda, app.selecionarAtividade, app.cancelarInscricao,
                app.inscricoes, app.usuarios, app.eventos, jwt)));
        server.createContext("/api/controle", envolver(new ControleHandler(app.realizarCheckIn,
                app.realizarCheckInManual, app.relatorioPresenca, app.relatorioInscritos,
                app.qrCodeService, app.inscricoes, jwt)));
        server.createContext("/api/avaliacoes", envolver(avaliacaoHandler));
        server.createContext("/api/questionarios", envolver(avaliacaoHandler));
        server.createContext("/api/certificados", envolver(certificadoHandler));
        server.createContext("/api/usuarios", envolver(
                new UsuarioHandler(app.atualizarUsuario, app.gerenciarUsuarios, app.usuarios, jwt)));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("[API] ouvindo em http://localhost:" + porta + "/api");
    }

    /** Garante que nenhuma exceção escape sem resposta (evita conexão fechada). */
    private static com.sun.net.httpserver.HttpHandler envolver(com.sun.net.httpserver.HttpHandler interno) {
        return exchange -> {
            try {
                interno.handle(exchange);
            } catch (Throwable t) {
                t.printStackTrace();
                HttpErrors.tratar(exchange, new IllegalStateException("Falha interna: " + t.getMessage(), t));
            }
        };
    }

    public void parar() {
        if (server != null) server.stop(0);
    }
}
