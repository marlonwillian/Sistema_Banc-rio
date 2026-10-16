package br.com.sistemabancario.persistence.jdbc;

import br.com.sistemabancario.domain.Cliente;
import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.persistence.ContaRepository;
import br.com.sistemabancario.persistence.PersistenceException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcContaRepository implements ContaRepository {
    private final ConnectionFactory connectionFactory;

    public JdbcContaRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void salvar(Conta conta) {
        String sql = "INSERT INTO contas (id, titular, saldo) VALUES (?, ?, ?)";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, conta.getId());
            statement.setString(2, conta.getTitular().nome());
            statement.setBigDecimal(3, conta.getSaldo());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("Nao foi possivel salvar a conta " + conta.getId() + ".", exception);
        }
    }

    @Override
    public Optional<Conta> buscarPorId(long id) {
        String sql = "SELECT id, titular, saldo FROM contas WHERE id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapearConta(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Nao foi possivel buscar a conta " + id + ".", exception);
        }
    }

    @Override
    public List<Conta> listarTodas() {
        String sql = "SELECT id, titular, saldo FROM contas ORDER BY id";
        List<Conta> contas = new ArrayList<>();
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                contas.add(mapearConta(result));
            }
            return List.copyOf(contas);
        } catch (SQLException exception) {
            throw new PersistenceException("Nao foi possivel listar as contas.", exception);
        }
    }

    @Override
    public void atualizarSaldo(long contaId, BigDecimal novoSaldo) {
        if (novoSaldo == null || novoSaldo.signum() < 0) {
            throw new IllegalArgumentException("O novo saldo nao pode ser negativo.");
        }
        String sql = "UPDATE contas SET saldo = ? WHERE id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, novoSaldo);
            statement.setLong(2, contaId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("A conta " + contaId + " nao existe.");
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Nao foi possivel atualizar a conta " + contaId + ".", exception);
        }
    }

    private static Conta mapearConta(ResultSet result) throws SQLException {
        long id = result.getLong("id");
        return new Conta(id, new Cliente(id, result.getString("titular")),
                result.getBigDecimal("saldo"));
    }
}
