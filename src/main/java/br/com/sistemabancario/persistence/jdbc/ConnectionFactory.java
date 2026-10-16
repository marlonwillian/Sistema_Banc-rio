package br.com.sistemabancario.persistence.jdbc;

import br.com.sistemabancario.persistence.PersistenceException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class ConnectionFactory {
    private static final String DEFAULT_URL =
            "jdbc:h2:file:./data/sistema-bancario;DB_CLOSE_ON_EXIT=FALSE";

    private final String jdbcUrl;

    public ConnectionFactory() {
        this(System.getProperty("sistema.banco.url", DEFAULT_URL));
    }

    public ConnectionFactory(String jdbcUrl) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "A URL JDBC e obrigatoria.");
    }

    public Connection getConnection() {
        try {
            if (jdbcUrl.startsWith("jdbc:h2:file:")) {
                Files.createDirectories(Path.of("./data").toAbsolutePath());
            }
            return DriverManager.getConnection(jdbcUrl, "sa", "");
        } catch (SQLException | IOException exception) {
            throw new PersistenceException("Nao foi possivel abrir conexao com o banco.", exception);
        }
    }

    public void inicializarSchema() {
        String schema;
        try (InputStream input = ConnectionFactory.class.getResourceAsStream("/schema.sql")) {
            if (input == null) {
                throw new PersistenceException("O recurso schema.sql nao foi encontrado.");
            }
            schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new PersistenceException("Nao foi possivel ler o schema do banco.", exception);
        }

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : schema.split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Nao foi possivel inicializar o schema do banco.", exception);
        }
    }
}
