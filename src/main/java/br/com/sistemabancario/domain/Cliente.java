package br.com.sistemabancario.domain;

import java.util.Objects;

public record Cliente(Long id, String nome) {
    public Cliente {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("O id do cliente deve ser positivo.");
        }
        Objects.requireNonNull(nome, "O nome do cliente e obrigatorio.");
        nome = nome.trim();
        if (nome.isEmpty()) {
            throw new IllegalArgumentException("O nome do cliente nao pode estar vazio.");
        }
    }
}
