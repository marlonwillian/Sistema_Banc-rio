package br.com.sistemabancario.simulacao.demo;

import java.math.BigDecimal;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public final class ContaInsegura {
    private BigDecimal saldo = BigDecimal.ZERO.setScale(2);

    public void depositarSemSincronizacao(BigDecimal valor, CyclicBarrier ambosLeram) {
        BigDecimal saldoLido = saldo;
        try {
            ambosLeram.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A demonstracao da corrida foi interrompida.", exception);
        } catch (BrokenBarrierException exception) {
            throw new IllegalStateException("A demonstracao da corrida foi interrompida.", exception);
        }
        saldo = saldoLido.add(valor);
    }

    public BigDecimal getSaldo() {
        return saldo;
    }
}
