package br.com.sistemabancario.service;

import br.com.sistemabancario.domain.Cliente;
import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.domain.Transacao;
import br.com.sistemabancario.persistence.ContaRepository;
import br.com.sistemabancario.persistence.PersistenceException;
import br.com.sistemabancario.persistence.TransacaoRepository;
import br.com.sistemabancario.persistence.memory.InMemoryContaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class BancoService {
    private final Map<Long, Conta> contas = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> locksPorConta = new ConcurrentHashMap<>();
    private final ContaRepository contaRepository;
    private final TransacaoRepository transacaoRepository;

    public BancoService(ContaRepository contaRepository, TransacaoRepository transacaoRepository) {
        this.contaRepository = contaRepository;
        this.transacaoRepository = transacaoRepository;
        contaRepository.listarTodas().forEach(conta -> contas.put(conta.getId(), conta));
    }

    public BancoService(List<Conta> contas, TransacaoRepository transacaoRepository) {
        this(criarRepositorioEmMemoria(contas), transacaoRepository);
    }

    public List<Conta> listarContas() {
        return contas.values().stream()
                .sorted((primeira, segunda) -> Long.compare(primeira.getId(), segunda.getId()))
                .toList();
    }

    public Optional<Conta> buscarContaPorId(long contaId) {
        return Optional.ofNullable(contas.get(contaId));
    }

    public Conta criarConta(long id, Cliente titular, BigDecimal saldoInicial) {
        Conta conta = new Conta(id, titular, saldoInicial);
        contaRepository.salvar(conta);
        if (contas.putIfAbsent(id, conta) != null) {
            throw new IllegalArgumentException("A conta " + id + " ja existe no servico.");
        }
        return conta;
    }

    public Transacao depositar(long contaId, BigDecimal valor) {
        ReentrantLock lock = lockPara(contaId);
        lock.lock();
        try {
            Conta conta = buscarConta(contaId);
            Transacao transacao = conta.depositar(valor);
            persistir(contaId, List.of(transacao));
            return transacao;
        } finally {
            lock.unlock();
        }
    }

    public Transacao sacar(long contaId, BigDecimal valor) {
        ReentrantLock lock = lockPara(contaId);
        lock.lock();
        try {
            Conta conta = buscarConta(contaId);
            Transacao transacao = conta.sacar(valor);
            persistir(contaId, List.of(transacao));
            return transacao;
        } finally {
            lock.unlock();
        }
    }

    public List<Transacao> transferir(long origemId, long destinoId, BigDecimal valor) {
        if (origemId == destinoId) {
            throw new IllegalArgumentException("Origem e destino devem ser contas diferentes.");
        }
        long primeiroId = Math.min(origemId, destinoId);
        long segundoId = Math.max(origemId, destinoId);
        ReentrantLock primeiroLock = lockPara(primeiroId);
        ReentrantLock segundoLock = lockPara(segundoId);
        primeiroLock.lock();
        segundoLock.lock();
        try {
            Conta origem = buscarConta(origemId);
            Conta destino = buscarConta(destinoId);
            List<Transacao> transacoes = origem.transferirPara(destino, valor);
            persistir(origemId, destinoId, transacoes);
            return transacoes;
        } finally {
            segundoLock.unlock();
            primeiroLock.unlock();
        }
    }

    public List<Transacao> buscarExtrato(long contaId) {
        buscarConta(contaId);
        return transacaoRepository.buscarPorConta(contaId);
    }

    private void persistir(long contaId, List<Transacao> transacoes) {
        try {
            transacaoRepository.salvarTodas(transacoes);
            atualizarSaldosPersistidos(contaId);
        } catch (PersistenceException exception) {
            restaurarContas(exception, contaId);
            throw exception;
        } catch (RuntimeException exception) {
            PersistenceException falha = new PersistenceException(
                    "Nao foi possivel concluir a persistencia da conta " + contaId + ".", exception);
            restaurarContas(falha, contaId);
            throw falha;
        }
    }

    private void persistir(long origemId, long destinoId, List<Transacao> transacoes) {
        try {
            transacaoRepository.salvarTodas(transacoes);
            atualizarSaldosPersistidos(origemId, destinoId);
        } catch (PersistenceException exception) {
            restaurarContas(exception, origemId, destinoId);
            throw exception;
        } catch (RuntimeException exception) {
            PersistenceException falha = new PersistenceException(
                    "Nao foi possivel concluir a persistencia da transferencia.", exception);
            restaurarContas(falha, origemId, destinoId);
            throw falha;
        }
    }

    private void atualizarSaldosPersistidos(long... ids) {
        if (transacaoRepository.persisteSaldosJuntoComTransacoes()) {
            return;
        }
        for (long id : ids) {
            contaRepository.atualizarSaldo(id, buscarConta(id).getSaldo());
        }
    }

    private void restaurarContas(PersistenceException original, long... ids) {
        for (long id : ids) {
            try {
                Conta persistida = contaRepository.buscarPorId(id)
                        .orElseThrow(() -> new IllegalStateException(
                                "A conta " + id + " nao foi encontrada para restauracao."));
                contas.put(id, persistida);
            } catch (RuntimeException falhaRestauracao) {
                original.addSuppressed(falhaRestauracao);
            }
        }
    }

    private Conta buscarConta(long contaId) {
        Conta conta = contas.get(contaId);
        if (conta == null) {
            throw new IllegalArgumentException("Conta " + contaId + " nao encontrada.");
        }
        return conta;
    }

    private ReentrantLock lockPara(long contaId) {
        return locksPorConta.computeIfAbsent(contaId, ignored -> new ReentrantLock());
    }

    private static ContaRepository criarRepositorioEmMemoria(List<Conta> contas) {
        ContaRepository repository = new InMemoryContaRepository();
        contas.forEach(repository::salvar);
        return repository;
    }
}
