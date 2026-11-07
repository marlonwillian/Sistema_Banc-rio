package br.com.sistemabancario.persistence.jdbc;

import br.com.sistemabancario.persistence.PersistenceException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class ConnectionFactory {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/sistema_bancario?serverTimezone=UTC";

    private final String jdbcUrl;
    private final String usuario;
    private final String senha;

    public ConnectionFactory() {
        this(
                configuracao("sistema.banco.url", "SISTEMA_BANCO_URL", DEFAULT_URL),
                configuracao("sistema.banco.usuario", "SISTEMA_BANCO_USUARIO", "root"),
                configuracao("sistema.banco.senha", "SISTEMA_BANCO_SENHA", ""));
    }

    public ConnectionFactory(String jdbcUrl, String usuario, String senha) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "A URL JDBC e obrigatoria.");
        this.usuario = Objects.requireNonNull(usuario, "O usuario do banco e obrigatorio.");
        this.senha = Objects.requireNonNull(senha, "A senha do banco e obrigatoria.");
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(jdbcUrl, usuario, senha);
        } catch (SQLException exception) {
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

    private static String configuracao(String propriedade, String variavelAmbiente, String padrao) {
        String valor = System.getProperty(propriedade);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(variavelAmbiente);
        }
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}
