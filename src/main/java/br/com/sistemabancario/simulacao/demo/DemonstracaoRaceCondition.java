package br.com.sistemabancario.simulacao.demo;

import br.com.sistemabancario.domain.Cliente;
import br.com.sistemabancario.domain.Conta;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class DemonstracaoRaceCondition {
    private static final BigDecimal VALOR = new BigDecimal("1.00");

    public ResultadoDemonstracaoCorrida executar() {
        ContaInsegura insegura = new ContaInsegura();
        Conta segura = new Conta(1, new Cliente(1L, "Demonstracao"), BigDecimal.ZERO);
        CyclicBarrier ambosLeram = new CyclicBarrier(2);
        CountDownLatch iniciarSegura = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> inseguroUm = executor.submit(
                    () -> insegura.depositarSemSincronizacao(VALOR, ambosLeram));
            Future<?> inseguroDois = executor.submit(
                    () -> insegura.depositarSemSincronizacao(VALOR, ambosLeram));
            inseguroUm.get(5, TimeUnit.SECONDS);
            inseguroDois.get(5, TimeUnit.SECONDS);

            Future<?> seguroUm = executor.submit(() -> depositarQuandoLiberado(segura, iniciarSegura));
            Future<?> seguroDois = executor.submit(() -> depositarQuandoLiberado(segura, iniciarSegura));
            iniciarSegura.countDown();
            seguroUm.get(5, TimeUnit.SECONDS);
            seguroDois.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A demonstracao da corrida foi interrompida.", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Nao foi possivel concluir a demonstracao da corrida.", exception);
        } finally {
            executor.shutdownNow();
        }

        return new ResultadoDemonstracaoCorrida(
                new BigDecimal("2.00"), insegura.getSaldo(), segura.getSaldo());
    }

    private static void depositarQuandoLiberado(Conta conta, CountDownLatch iniciar) {
        try {
            iniciar.await();
            conta.depositar(VALOR);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("O deposito demonstrativo foi interrompido.", exception);
        }
    }
}
