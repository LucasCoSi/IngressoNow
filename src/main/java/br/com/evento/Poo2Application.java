package br.com.evento;

import br.com.evento.adapters.in.http.ServidorApi;
import br.com.evento.infra.AppConfig;
import br.com.evento.infra.ConnectionFactory;
import br.com.evento.infra.FabricaAplicacao;
import br.com.evento.infra.MigradorSql;

/**
 * Ponto de entrada Java SE (sem Spring Boot).
 * Uso: java -jar plataforma-eventos.jar [--headless]
 * Sem flag: sobe a API e abre o desktop JavaFX (Fase 4).
 * Com --headless: sobe só a API (útil p/ Docker e p/ o protótipo web).
 */
public class Poo2Application {

    public static void main(String[] args) throws Exception {
        boolean headless = args != null && java.util.Arrays.asList(args).contains("--headless");
        AppConfig config = new AppConfig();
        ConnectionFactory connections = new ConnectionFactory(config.dbUrl(), config.dbUser(), config.dbPass());
        new MigradorSql(connections).migrar();
        FabricaAplicacao app = new FabricaAplicacao(config);
        ServidorApi api = new ServidorApi(app);
        api.iniciar(config.serverPort());
        if (headless) {
            System.out.println("[App] modo headless — API ativa, sem interface desktop.");
            Thread.currentThread().join();
        } else {
            // D-01: desktop JavaFX no mesmo processo, mesmo banco e use cases (RN-19)
            br.com.evento.adapters.in.desktop.DesktopApp.conectar(app);
            br.com.evento.adapters.in.desktop.DesktopApp.abrir(args);
        }
    }
}
