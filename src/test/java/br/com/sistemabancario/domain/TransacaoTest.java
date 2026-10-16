package br.com.sistemabancario.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransacaoTest {
    @Test
    void criaRegistroImutavelComValorMonetarioPadronizado() {
        LocalDateTime horario = LocalDateTime.of(2026, 10, 16, 19, 0);
        Transacao transacao = new Transacao(
                1L, TipoTransacao.DEPOSITO, new BigDecimal("25"), horario, 1L, null);

        assertEquals(new BigDecimal("25.00"), transacao.getValor());
        assertEquals(horario, transacao.getHorario());
        assertEquals(1L, transacao.getContaId());
        assertNull(transacao.getContaContraparteId());
    }

    @Test
    void rejeitaValorDeTransacaoNaoPositivo() {
        assertThrows(ValorInvalidoException.class, () -> new Transacao(
                1L,
                TipoTransacao.DEPOSITO,
                BigDecimal.ZERO,
                LocalDateTime.now(),
                1L,
                null));
    }

    @Test
    void rejeitaMaisDeDuasCasasDecimais() {
        assertThrows(ValorInvalidoException.class, () -> new Transacao(
                1L,
                TipoTransacao.DEPOSITO,
                new BigDecimal("1.001"),
                LocalDateTime.now(),
                1L,
                null));
    }
}
