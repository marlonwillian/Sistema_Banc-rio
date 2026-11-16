package br.com.sistemabancario.persistence;

import br.com.sistemabancario.domain.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {
    void salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(long id);

    Optional<Usuario> buscarPorLogin(String login);

    List<Usuario> listarTodos();
}
