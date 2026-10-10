package br.com.sistemabancario.domain;

public class OperacaoInvalidaException extends IllegalArgumentException {
    public OperacaoInvalidaException(String message) {
        super(message);
    }
}
