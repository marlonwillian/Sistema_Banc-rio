package br.com.sistemabancario.simulacao.demo;

import java.math.BigDecimal;

public record ResultadoDemonstracaoCorrida(
        BigDecimal saldoEsperado,
        BigDecimal saldoInseguro,
        BigDecimal saldoSincronizado) {
}
