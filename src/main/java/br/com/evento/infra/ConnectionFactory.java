package br.com.evento.infra;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Fábrica de conexões JDBC puras (DriverManager, try-with-resources).
 * Substitui o HikariCP/Spring Data JPA.
 */
public class ConnectionFactory {

    private final String url;
    private final String user;
    private final String pass;

    public ConnectionFactory(String url, String user, String pass) {
        this.url = url;
        this.user = user;
        this.pass = pass;
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }
}
