package br.com.sistemabancario.domain;

import java.util.Objects;

public record Usuario(Long id, String login, String senhaHash, Perfil perfil, Long contaId) {
    public Usuario {
        Objects.requireNonNull(login, "O login do usuario e obrigatorio.");
        login = login.trim();
        if (login.isBlank()) {
            throw new IllegalArgumentException("O login do usuario nao pode estar vazio.");
        }
        Objects.requireNonNull(senhaHash, "O hash da senha e obrigatorio.");
        Objects.requireNonNull(perfil, "O perfil do usuario e obrigatorio.");
        if (perfil == Perfil.USUARIO && (contaId == null || contaId <= 0)) {
            throw new IllegalArgumentException("Usuario comum deve estar vinculado a uma conta.");
        }
        if (perfil == Perfil.ADMIN && contaId != null && contaId <= 0) {
            throw new IllegalArgumentException("A conta do administrador deve ser positiva.");
        }
    }

    public boolean eAdmin() {
        return perfil == Perfil.ADMIN;
    }

    public boolean eUsuario() {
        return perfil == Perfil.USUARIO;
    }
}
