package br.com.sistemabancario.persistence.memory;

import br.com.sistemabancario.domain.Cliente;
import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.domain.ValorInvalidoException;
import br.com.sistemabancario.persistence.ContaRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryContaRepository implements ContaRepository {
    private final ConcurrentHashMap<Long, DadosConta> contas = new ConcurrentHashMap<>();

    @Override
    public void salvar(Conta conta) {
        DadosConta dados = new DadosConta(
                conta.getId(), conta.getTitular().nome(), conta.getSaldo());
        if (contas.putIfAbsent(conta.getId(), dados) != null) {
            throw new IllegalArgumentException("A conta " + conta.getId() + " ja existe.");
        }
    }

    @Override
    public Optional<Conta> buscarPorId(long id) {
        return Optional.ofNullable(contas.get(id)).map(DadosConta::paraConta);
    }

    @Override
    public List<Conta> listarTodas() {
        return contas.values().stream()
                .sorted(Comparator.comparingLong(DadosConta::id))
                .map(DadosConta::paraConta)
                .toList();
    }

    @Override
    public void atualizarSaldo(long contaId, BigDecimal novoSaldo) {
        if (novoSaldo == null || novoSaldo.signum() < 0) {
            throw new ValorInvalidoException("O saldo atualizado nao pode ser negativo.");
        }
        contas.compute(contaId, (id, atual) -> {
            if (atual == null) {
                throw new IllegalArgumentException("A conta " + id + " nao existe.");
            }
            return new DadosConta(id, atual.titular(), novoSaldo);
        });
    }

    private record DadosConta(long id, String titular, BigDecimal saldo) {
        private Conta paraConta() {
            return new Conta(id, new Cliente(id, titular), saldo);
        }
    }
}
