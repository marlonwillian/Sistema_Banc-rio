package br.com.sistemabancario.simulacao;

import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.service.BancoService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class SimulacaoRunner {
    private static final long TIMEOUT_SEGUNDOS = 60;

    private final BancoService bancoService;

    public SimulacaoRunner(BancoService bancoService) {
        this.bancoService = bancoService;
    }

    public ResultadoSimulacao executar(
            int quantidadeThreads, int operacoesPorThread, BigDecimal valor) {
        if (quantidadeThreads <= 0 || operacoesPorThread <= 0) {
            throw new IllegalArgumentException("Threads e operacoes por thread devem ser positivos.");
        }
        if (bancoService.listarContas().size() < 2) {
            throw new IllegalStateException("A simulacao requer pelo menos duas contas.");
        }
        List<Long> ids = bancoService.listarContas().stream().map(Conta::getId).toList();
        List<OperacaoSimulada> operacoes = new ArrayList<>();
        int total = quantidadeThreads * operacoesPorThread;
        for (int indice = 0; indice < total; indice++) {
            long origemId = ids.get(indice % ids.size());
            long destinoId = ids.get((indice + 1) % ids.size());
            TipoOperacao tipo = switch (indice % 3) {
                case 0 -> TipoOperacao.DEPOSITO;
                case 1 -> TipoOperacao.SAQUE;
                default -> TipoOperacao.TRANSFERENCIA;
            };
            operacoes.add(new OperacaoSimulada(
                    tipo, origemId,
                    tipo == TipoOperacao.TRANSFERENCIA ? destinoId : null,
                    valor));
        }
        return executar(operacoes, quantidadeThreads);
    }

    public ResultadoSimulacao executar(List<OperacaoSimulada> operacoes, int quantidadeThreads) {
        if (quantidadeThreads <= 0) {
            throw new IllegalArgumentException("A quantidade de threads deve ser positiva.");
        }
        List<Conta> contas = bancoService.listarContas();
        if (contas.isEmpty()) {
            throw new IllegalStateException("Nao ha contas cadastradas para simular.");
        }

        Map<Long, BigDecimal> saldosIniciais = new HashMap<>();
        contas.forEach(conta -> saldosIniciais.put(conta.getId(), conta.getSaldo()));
        int totalThreads = Math.min(quantidadeThreads, Math.max(1, operacoes.size()));
        List<List<OperacaoSimulada>> distribuicao = new ArrayList<>();
        for (int i = 0; i < totalThreads; i++) {
            distribuicao.add(new ArrayList<>());
        }
        for (int i = 0; i < operacoes.size(); i++) {
            distribuicao.get(i % totalThreads).add(operacoes.get(i));
        }

        CountDownLatch prontas = new CountDownLatch(totalThreads);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        List<OperacaoBancariaThread> tarefas = new ArrayList<>();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (List<OperacaoSimulada> grupo : distribuicao) {
                OperacaoBancariaThread tarefa =
                        new OperacaoBancariaThread(bancoService, grupo, prontas, iniciar);
                tarefas.add(tarefa);
                futures.add(executor.submit(tarefa));
            }
            if (!prontas.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("As threads da simulacao nao ficaram prontas a tempo.");
            }
            iniciar.countDown();
            for (Future<?> future : futures) {
                future.get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A simulacao foi interrompida.", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Uma operacao da simulacao falhou.", exception.getCause());
        } catch (java.util.concurrent.TimeoutException exception) {
            throw new IllegalStateException("A simulacao excedeu o tempo limite.", exception);
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }

        List<OperacaoSimulada> operacoesRealizadas = tarefas.stream()
                .flatMap(tarefa -> tarefa.getOperacoesRealizadasComSucesso().stream())
                .toList();
        Map<Long, BigDecimal> esperados = new HashMap<>(saldosIniciais);
        for (OperacaoSimulada operacao : operacoesRealizadas) {
            BigDecimal variacao = operacao.valor();
            if (operacao.tipo() == TipoOperacao.SAQUE
                    || operacao.tipo() == TipoOperacao.TRANSFERENCIA) {
                variacao = variacao.negate();
            }
            esperados.put(
                    operacao.contaOrigemId(),
                    esperados.get(operacao.contaOrigemId()).add(variacao));
            if (operacao.tipo() == TipoOperacao.TRANSFERENCIA) {
                long destinoId = operacao.contaDestinoId();
                esperados.put(destinoId, esperados.get(destinoId).add(operacao.valor()));
            }
        }
        Map<Long, BigDecimal> obtidos = new HashMap<>();
        contas.forEach(conta -> obtidos.put(
                conta.getId(),
                bancoService.buscarContaPorId(conta.getId()).orElseThrow().getSaldo()));

        int realizadas = tarefas.stream().mapToInt(OperacaoBancariaThread::getRealizadas).sum();
        int recusadas = tarefas.stream()
                .mapToInt(OperacaoBancariaThread::getRecusadasPorSaldo)
                .sum();
        List<String> falhas = tarefas.stream()
                .flatMap(tarefa -> tarefa.getFalhas().stream())
                .toList();
        return new ResultadoSimulacao(
                operacoes.size(), realizadas, recusadas, esperados, obtidos, falhas);
    }
}
