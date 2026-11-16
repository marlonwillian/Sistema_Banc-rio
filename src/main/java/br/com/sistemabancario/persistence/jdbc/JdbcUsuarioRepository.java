package br.com.sistemabancario.persistence.jdbc;

import br.com.sistemabancario.domain.Perfil;
import br.com.sistemabancario.domain.Usuario;
import br.com.sistemabancario.persistence.PersistenceException;
import br.com.sistemabancario.persistence.UsuarioRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcUsuarioRepository implements UsuarioRepository {
    private final ConnectionFactory connectionFactory;

    public JdbcUsuarioRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void salvar(Usuario usuario) {
        String sql = "INSERT INTO usuarios (login, senha_hash, perfil, conta_id) VALUES (?, ?, ?, ?)";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, usuario.login());
            statement.setString(2, usuario.senhaHash());
            statement.setString(3, usuario.perfil().name());
            if (usuario.contaId() == null) {
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(4, usuario.contaId());
            }
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new PersistenceException("Nao foi possivel salvar o usuario " + usuario.login() + ".", exception);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(long id) {
        String sql = "SELECT id, login, senha_hash, perfil, conta_id FROM usuarios WHERE id = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapearUsuario(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Nao foi possivel buscar o usuario " + id + ".", exception);
        }
    }

    @Override
    public Optional<Usuario> buscarPorLogin(String login) {
        String sql = "SELECT id, login, senha_hash, perfil, conta_id FROM usuarios WHERE login = ?";
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapearUsuario(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Nao foi possivel buscar o usuario " + login + ".", exception);
        }
    }

    @Override
    public List<Usuario> listarTodos() {
        String sql = "SELECT id, login, senha_hash, perfil, conta_id FROM usuarios ORDER BY id";
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                usuarios.add(mapearUsuario(result));
            }
            return List.copyOf(usuarios);
        } catch (SQLException exception) {
            throw new PersistenceException("Nao foi possivel listar os usuarios.", exception);
        }
    }

    private static Usuario mapearUsuario(ResultSet result) throws SQLException {
        String perfil = result.getString("perfil");
        Long contaId = result.getObject("conta_id", Long.class);
        return new Usuario(
                result.getLong("id"),
                result.getString("login"),
                result.getString("senha_hash"),
                Perfil.valueOf(perfil),
                contaId);
    }
}
