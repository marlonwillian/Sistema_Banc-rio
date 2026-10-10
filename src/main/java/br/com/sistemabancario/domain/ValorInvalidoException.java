package br.com.sistemabancario.domain;

public class ValorInvalidoException extends IllegalArgumentException {
    public ValorInvalidoException(String message) {
        super(message);
    }
}
