package br.com.sistemabancario.persistence.jdbc;

import br.com.sistemabancario.domain.TipoTransacao;
import br.com.sistemabancario.domain.Transacao;
import br.com.sistemabancario.persistence.PersistenceException;
import br.com.sistemabancario.persistence.TransacaoRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class JdbcTransacaoRepository implements TransacaoRepository {
    private static final String INSERT = """
            INSERT INTO transacoes
                (tipo, valor, horario, conta_id, conta_contraparte_id)
            VALUES (?, ?, ?, ?, ?)
            """;

    private final ConnectionFactory connectionFactory;

    public JdbcTransacaoRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void salvar(Transacao transacao) {
        salvarTodas(List.of(transacao));
    }

    @Override
    public void salvarTodas(List<Transacao> transacoes) {
        if (transacoes.isEmpty()) {
            return;
        }

        try (Connection connection = connectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (Transacao transacao : transacoes) {
                    aplicarAlteracaoSaldo(connection, transacao);
                    inserirTransacao(connection, transacao);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Nao foi possivel persistir a operacao bancaria.", exception);
        } catch (RuntimeException exception) {
            if (exception instanceof PersistenceException persistenceException) {
                throw persistenceException;
            }
            throw new PersistenceException("A operacao bancaria foi revertida no banco.", exception);
        }
    }

    @Override
    public boolean persisteSaldosJuntoComTransacoes() {
        return true;
    }

    @Override
    public List<Transacao> buscarPorConta(long contaId) {
        String sql = """
                SELECT id, tipo, valor, horario, conta_id, conta_contraparte_id
                FROM transacoes
                WHERE conta_id = ?
                ORDER BY horario, id
                """;
        List<Transacao> transacoes = new ArrayList<>();
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, contaId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Long contraparte = result.getObject("conta_contraparte_id", Long.class);
                    transacoes.add(new Transacao(
                            result.getLong("id"),
                            TipoTransacao.valueOf(result.getString("tipo")),
                            result.getBigDecimal("valor"),
                            result.getTimestamp("horario").toLocalDateTime(),
                            result.getLong("conta_id"),
                            contraparte));
                }
            }
            return List.copyOf(transacoes);
        } catch (SQLException exception) {
            throw new PersistenceException("Nao foi possivel buscar o extrato da conta " + contaId + ".", exception);
        }
    }

    private static void aplicarAlteracaoSaldo(Connection connection, Transacao transacao)
            throws SQLException {
        boolean credito = transacao.getTipo() == TipoTransacao.DEPOSITO
                || transacao.getTipo() == TipoTransacao.TRANSFERENCIA_RECEBIDA;
        String sql = credito
                ? "UPDATE contas SET saldo = saldo + ? WHERE id = ?"
                : "UPDATE contas SET saldo = saldo - ? WHERE id = ? AND saldo >= ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, transacao.getValor());
            statement.setLong(2, transacao.getContaId());
            if (!credito) {
                statement.setBigDecimal(3, transacao.getValor());
            }
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Conta inexistente ou saldo insuficiente no banco: "
                        + transacao.getContaId());
            }
        }
    }

    private static void inserirTransacao(Connection connection, Transacao transacao) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
            statement.setString(1, transacao.getTipo().name());
            statement.setBigDecimal(2, transacao.getValor());
            statement.setTimestamp(3, Timestamp.valueOf(transacao.getHorario()));
            statement.setLong(4, transacao.getContaId());
            if (transacao.getContaContraparteId() == null) {
                statement.setNull(5, java.sql.Types.BIGINT);
            } else {
                statement.setLong(5, transacao.getContaContraparteId());
            }
            statement.executeUpdate();
        }
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }
}
