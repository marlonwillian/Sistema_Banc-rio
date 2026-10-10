package br.com.sistemabancario.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContaTest {
    private final Cliente cliente = new Cliente(1L, "Ana");

    @Test
    void depositarAtualizaSaldoERegistraTransacao() {
        Conta conta = new Conta(1, cliente, new BigDecimal("10.00"));

        Transacao transacao = conta.depositar(new BigDecimal("5.50"));

        assertEquals(new BigDecimal("15.50"), conta.getSaldo());
        assertEquals(TipoTransacao.DEPOSITO, transacao.getTipo());
        assertEquals(1, conta.getExtrato().size());
    }

    @Test
    void sacarComSaldoInsuficienteNaoAlteraConta() {
        Conta conta = new Conta(1, cliente, new BigDecimal("5.00"));

        assertThrows(SaldoInsuficienteException.class,
                () -> conta.sacar(new BigDecimal("5.01")));

        assertEquals(new BigDecimal("5.00"), conta.getSaldo());
        assertEquals(0, conta.getExtrato().size());
    }

    @Test
    void rejeitaValorComMaisDeDuasCasasDecimais() {
        Conta conta = new Conta(1, cliente, BigDecimal.ZERO);

        assertThrows(ValorInvalidoException.class,
                () -> conta.depositar(new BigDecimal("1.001")));
        assertEquals(BigDecimal.ZERO.setScale(2), conta.getSaldo());
    }

    @Test
    void transferenciaAtualizaAmbasAsContasESeusExtratos() {
        Conta origem = new Conta(1, cliente, new BigDecimal("20.00"));
        Conta destino = new Conta(2, new Cliente(2L, "Bruno"), BigDecimal.ZERO);

        var transacoes = origem.transferirPara(destino, new BigDecimal("7.25"));

        assertEquals(new BigDecimal("12.75"), origem.getSaldo());
        assertEquals(new BigDecimal("7.25"), destino.getSaldo());
        assertEquals(TipoTransacao.TRANSFERENCIA_ENVIADA, transacoes.get(0).getTipo());
        assertEquals(TipoTransacao.TRANSFERENCIA_RECEBIDA, transacoes.get(1).getTipo());
        assertEquals(1, origem.getExtrato().size());
        assertEquals(1, destino.getExtrato().size());
    }

    @Test
    void transferenciaParaMesmaContaERejeitada() {
        Conta conta = new Conta(1, cliente, new BigDecimal("20.00"));

        assertThrows(OperacaoInvalidaException.class,
                () -> conta.transferirPara(conta, BigDecimal.ONE));
        assertEquals(new BigDecimal("20.00"), conta.getSaldo());
    }

    @Test
    void transferenciaEntreObjetosComMesmoIdERejeitada() {
        Conta origem = new Conta(1, cliente, new BigDecimal("20.00"));
        Conta destino = new Conta(1, new Cliente(2L, "Bruno"), BigDecimal.ZERO);

        assertThrows(OperacaoInvalidaException.class,
                () -> origem.transferirPara(destino, BigDecimal.ONE));
        assertEquals(new BigDecimal("20.00"), origem.getSaldo());
        assertEquals(BigDecimal.ZERO.setScale(2), destino.getSaldo());
    }
}
