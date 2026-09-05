package br.com.evento.infra;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuração via config.properties + variáveis de ambiente.
 * Substitui o application.properties / @Value do Spring.
 */
public class AppConfig {

    private final Properties props = new Properties();

    public AppConfig() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler config.properties", e);
        }
    }

    public String dbUrl() {
        return env("DB_URL", props.getProperty("db.url", "jdbc:postgresql://localhost:5432/poo2"));
    }

    public String dbUser() {
        return env("DB_USER", props.getProperty("db.user", "postgres"));
    }

    public String dbPass() {
        return env("DB_PASS", props.getProperty("db.pass", "postgres"));
    }

    public String jwtSecret() {
        return env("JWT_SECRET", props.getProperty("jwt.secret", "troque-este-segredo"));
    }

    public long jwtExpirationMs() {
        String v = env("JWT_EXPIRATION", props.getProperty("jwt.expiration", "86400000"));
        return Long.parseLong(v);
    }

    public int serverPort() {
        String v = env("PORT", props.getProperty("server.port", "8080"));
        return Integer.parseInt(v);
    }

    private static String env(String name, String def) {
        // PowerShell: $env:DB_URL=... define variável de *processo*, que System.getenv enxerga.
        String v = System.getenv(name);
        if (v == null || v.isBlank()) v = System.getProperty(name);
        return (v == null || v.isBlank()) ? def : v;
    }
}
