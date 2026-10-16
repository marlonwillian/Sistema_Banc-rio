package br.com.sistemabancario.persistence.jdbc;

import br.com.sistemabancario.domain.Cliente;
import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.persistence.PersistenceException;
import br.com.sistemabancario.service.BancoService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcPersistenceTest {
    @Test
    void contasESaidasDeTransacaoSobrevivemANovaInstanciaDoServico() {
        ConnectionFactory factory = criarBanco();
        JdbcContaRepository contas = new JdbcContaRepository(factory);
        JdbcTransacaoRepository transacoes = new JdbcTransacaoRepository(factory);
        contas.salvar(new Conta(1, new Cliente(1L, "Ana"), new BigDecimal("100.00")));
        contas.salvar(new Conta(2, new Cliente(2L, "Bruno"), new BigDecimal("20.00")));

        BancoService service = new BancoService(contas, transacoes);
        service.depositar(1, new BigDecimal("5.00"));
        service.sacar(1, new BigDecimal("10.00"));
        service.transferir(1, 2, new BigDecimal("15.00"));

        BancoService reaberto = new BancoService(
                new JdbcContaRepository(factory), new JdbcTransacaoRepository(factory));

        assertEquals(new BigDecimal("80.00"),
                reaberto.buscarContaPorId(1).orElseThrow().getSaldo());
        assertEquals(new BigDecimal("35.00"),
                reaberto.buscarContaPorId(2).orElseThrow().getSaldo());
        assertEquals(3, reaberto.buscarExtrato(1).size());
        assertEquals(1, reaberto.buscarExtrato(2).size());
    }

    @Test
    void falhaAoPersistirReverteOperacaoNoBancoERestauraContaEmMemoria() throws Exception {
        ConnectionFactory factory = criarBanco();
        JdbcContaRepository contas = new JdbcContaRepository(factory);
        JdbcTransacaoRepository transacoes = new JdbcTransacaoRepository(factory);
        contas.salvar(new Conta(1, new Cliente(1L, "Ana"), new BigDecimal("10.00")));
        BancoService service = new BancoService(contas, transacoes);
        try (Connection connection = factory.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE transacoes");
        }

        assertThrows(PersistenceException.class,
                () -> service.depositar(1, new BigDecimal("5.00")));

        assertEquals(new BigDecimal("10.00"),
                service.buscarContaPorId(1).orElseThrow().getSaldo());
        assertEquals(new BigDecimal("10.00"),
                contas.buscarPorId(1).orElseThrow().getSaldo());
    }

    @Test
    void depositosJDBCConcorrentesPersistemSaldoEExtratoCorretos() throws Exception {
        ConnectionFactory factory = criarBanco();
        JdbcContaRepository contas = new JdbcContaRepository(factory);
        JdbcTransacaoRepository transacoes = new JdbcTransacaoRepository(factory);
        contas.salvar(new Conta(1, new Cliente(1L, "Ana"), BigDecimal.ZERO));
        BancoService service = new BancoService(contas, transacoes);
        int quantidadeThreads = 4;
        int depositosPorThread = 10;
        CountDownLatch prontas = new CountDownLatch(quantidadeThreads);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(quantidadeThreads);

        try {
            Future<?>[] tarefas = new Future<?>[quantidadeThreads];
            for (int i = 0; i < quantidadeThreads; i++) {
                tarefas[i] = executor.submit(() -> {
                    prontas.countDown();
                    iniciar.await();
                    for (int deposito = 0; deposito < depositosPorThread; deposito++) {
                        service.depositar(1, new BigDecimal("0.01"));
                    }
                    return null;
                });
            }
            assertTrue(prontas.await(5, TimeUnit.SECONDS));
            iniciar.countDown();
            for (Future<?> tarefa : tarefas) {
                tarefa.get(20, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        BancoService recarregado = new BancoService(
                new JdbcContaRepository(factory), new JdbcTransacaoRepository(factory));
        assertEquals(new BigDecimal("0.40"),
                recarregado.buscarContaPorId(1).orElseThrow().getSaldo());
        assertEquals(40, recarregado.buscarExtrato(1).size());
    }

    @Test
    void transferenciasJDBCEmSentidosOpostosPersistemSemDeadlock() throws Exception {
        ConnectionFactory factory = criarBanco();
        JdbcContaRepository contas = new JdbcContaRepository(factory);
        JdbcTransacaoRepository transacoes = new JdbcTransacaoRepository(factory);
        contas.salvar(new Conta(1, new Cliente(1L, "Ana"), new BigDecimal("100.00")));
        contas.salvar(new Conta(2, new Cliente(2L, "Bruno"), new BigDecimal("100.00")));
        BancoService service = new BancoService(contas, transacoes);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2, tarefa -> {
            Thread thread = new Thread(tarefa);
            thread.setDaemon(true);
            return thread;
        });

        try {
            Future<?> ida = executor.submit(() -> transferirRepetidamente(service, 1, 2, iniciar));
            Future<?> volta = executor.submit(() -> transferirRepetidamente(service, 2, 1, iniciar));
            iniciar.countDown();
            ida.get(30, TimeUnit.SECONDS);
            volta.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        BancoService recarregado = new BancoService(
                new JdbcContaRepository(factory), new JdbcTransacaoRepository(factory));
        assertEquals(new BigDecimal("200.00"),
                recarregado.buscarContaPorId(1).orElseThrow().getSaldo()
                        .add(recarregado.buscarContaPorId(2).orElseThrow().getSaldo()));
        assertEquals(160, recarregado.buscarExtrato(1).size());
        assertEquals(160, recarregado.buscarExtrato(2).size());
    }

    private static void transferirRepetidamente(
            BancoService service, long origem, long destino, CountDownLatch iniciar) {
        try {
            iniciar.await();
            for (int operacao = 0; operacao < 80; operacao++) {
                service.transferir(origem, destino, new BigDecimal("0.01"));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("O teste de transferencia foi interrompido.", exception);
        }
    }

    private static ConnectionFactory criarBanco() {
        ConnectionFactory factory = new ConnectionFactory(
                "jdbc:h2:mem:bank_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        factory.inicializarSchema();
        return factory;
    }
}
