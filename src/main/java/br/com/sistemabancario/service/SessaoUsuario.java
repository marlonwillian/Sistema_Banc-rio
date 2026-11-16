package br.com.sistemabancario.service;

import br.com.sistemabancario.domain.Perfil;
import br.com.sistemabancario.domain.Usuario;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class SessaoUsuario {
    private static final AtomicReference<Usuario> USUARIO_ATUAL = new AtomicReference<>();

    private SessaoUsuario() {
    }

    public static void abrir(Usuario usuario) {
        USUARIO_ATUAL.set(Objects.requireNonNull(usuario, "O usuario autenticado e obrigatorio."));
    }

    public static void fechar() {
        USUARIO_ATUAL.set(null);
    }

    public static Optional<Usuario> getUsuarioAtual() {
        return Optional.ofNullable(USUARIO_ATUAL.get());
    }

    public static boolean isAdmin() {
        return getUsuarioAtual().map(Usuario::perfil).map(Perfil.ADMIN::equals).orElse(false);
    }

    public static boolean isUsuario() {
        return getUsuarioAtual().map(Usuario::perfil).map(Perfil.USUARIO::equals).orElse(false);
    }
}
