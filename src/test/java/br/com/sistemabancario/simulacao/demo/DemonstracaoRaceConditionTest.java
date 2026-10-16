package br.com.sistemabancario.simulacao.demo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DemonstracaoRaceConditionTest {
    @Test
    void demonstraSaldoPerdidoECorrecaoComSincronizacao() {
        ResultadoDemonstracaoCorrida resultado =
                new DemonstracaoRaceCondition().executar();

        assertEquals(new BigDecimal("2.00"), resultado.saldoEsperado());
        assertEquals(new BigDecimal("1.00"), resultado.saldoInseguro());
        assertEquals(resultado.saldoEsperado(), resultado.saldoSincronizado());
    }
}
