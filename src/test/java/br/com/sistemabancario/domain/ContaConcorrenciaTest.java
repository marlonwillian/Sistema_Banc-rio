package br.com.sistemabancario.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContaConcorrenciaTest {
    @Test
    void depositosConcorrentesPreservamSaldoExato() throws Exception {
        int quantidadeThreads = 10;
        int depositosPorThread = 1_000;
        Conta conta = new Conta(1, new Cliente(1L, "Ana"), BigDecimal.ZERO);
        ExecutorService executor = Executors.newFixedThreadPool(quantidadeThreads);
        CountDownLatch pronta = new CountDownLatch(quantidadeThreads);
        CountDownLatch iniciar = new CountDownLatch(1);

        try {
            Future<?>[] tarefas = new Future<?>[quantidadeThreads];
            for (int i = 0; i < quantidadeThreads; i++) {
                tarefas[i] = executor.submit(() -> {
                    pronta.countDown();
                    iniciar.await();
                    for (int operacao = 0; operacao < depositosPorThread; operacao++) {
                        conta.depositar(new BigDecimal("0.01"));
                    }
                    return null;
                });
            }

            assertTrue(pronta.await(5, TimeUnit.SECONDS), "As threads nao ficaram prontas a tempo.");
            iniciar.countDown();
            for (Future<?> tarefa : tarefas) {
                tarefa.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(new BigDecimal("100.00"), conta.getSaldo());
        assertEquals(quantidadeThreads * depositosPorThread, conta.getExtrato().size());
    }

    @Test
    void transferenciasEmSentidosOpostosTerminamSemDeadlockEConservamSaldo() throws Exception {
        Conta primeira = new Conta(1, new Cliente(1L, "Ana"), new BigDecimal("100.00"));
        Conta segunda = new Conta(2, new Cliente(2L, "Bruno"), new BigDecimal("100.00"));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        try {
            Future<?> ida = executor.submit(() -> transferirRepetidamente(
                    primeira, segunda, iniciar));
            Future<?> volta = executor.submit(() -> transferirRepetidamente(
                    segunda, primeira, iniciar));

            iniciar.countDown();
            ida.get(10, TimeUnit.SECONDS);
            volta.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(new BigDecimal("200.00"), primeira.getSaldo().add(segunda.getSaldo()));
    }

    private static void transferirRepetidamente(
            Conta origem, Conta destino, CountDownLatch iniciar) {
        try {
            iniciar.await();
            for (int operacao = 0; operacao < 1_000; operacao++) {
                origem.transferirPara(destino, new BigDecimal("0.01"));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        }
    }
}
