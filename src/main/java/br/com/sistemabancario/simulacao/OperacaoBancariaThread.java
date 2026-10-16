package br.com.sistemabancario.simulacao;

import br.com.sistemabancario.domain.SaldoInsuficienteException;
import br.com.sistemabancario.service.BancoService;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class OperacaoBancariaThread implements Runnable {
    private final BancoService bancoService;
    private final List<OperacaoSimulada> operacoes;
    private final CountDownLatch prontas;
    private final CountDownLatch iniciar;
    private final AtomicInteger realizadas = new AtomicInteger();
    private final AtomicInteger recusadasPorSaldo = new AtomicInteger();
    private final ConcurrentLinkedQueue<OperacaoSimulada> realizadasComSucesso =
            new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> falhas = new ConcurrentLinkedQueue<>();

    public OperacaoBancariaThread(
            BancoService bancoService,
            List<OperacaoSimulada> operacoes,
            CountDownLatch prontas,
            CountDownLatch iniciar) {
        this.bancoService = bancoService;
        this.operacoes = List.copyOf(operacoes);
        this.prontas = prontas;
        this.iniciar = iniciar;
    }

    @Override
    public void run() {
        prontas.countDown();
        try {
            iniciar.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A thread bancaria foi interrompida antes da simulacao.", exception);
        }

        for (OperacaoSimulada operacao : operacoes) {
            try {
                executar(operacao);
                realizadas.incrementAndGet();
                realizadasComSucesso.add(operacao);
            } catch (SaldoInsuficienteException exception) {
                recusadasPorSaldo.incrementAndGet();
                falhas.add(exception.getMessage());
            }
        }
    }

    private void executar(OperacaoSimulada operacao) {
        switch (operacao.tipo()) {
            case DEPOSITO -> bancoService.depositar(operacao.contaOrigemId(), operacao.valor());
            case SAQUE -> bancoService.sacar(operacao.contaOrigemId(), operacao.valor());
            case TRANSFERENCIA -> bancoService.transferir(
                    operacao.contaOrigemId(),
                    operacao.contaDestinoId(),
                    operacao.valor());
        }
    }

    public int getRealizadas() {
        return realizadas.get();
    }

    public int getRecusadasPorSaldo() {
        return recusadasPorSaldo.get();
    }

    public List<OperacaoSimulada> getOperacoesRealizadasComSucesso() {
        return List.copyOf(realizadasComSucesso);
    }

    public List<String> getFalhas() {
        return List.copyOf(falhas);
    }
}
