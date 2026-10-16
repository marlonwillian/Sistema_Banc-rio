package br.com.sistemabancario.io;

public class ExportacaoException extends RuntimeException {
    public ExportacaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
