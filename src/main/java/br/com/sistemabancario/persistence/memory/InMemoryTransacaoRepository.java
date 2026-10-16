package br.com.sistemabancario.persistence.memory;

import br.com.sistemabancario.domain.Transacao;
import br.com.sistemabancario.persistence.TransacaoRepository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryTransacaoRepository implements TransacaoRepository {
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<Transacao>> transacoes =
            new ConcurrentHashMap<>();

    @Override
    public void salvar(Transacao transacao) {
        transacoes.computeIfAbsent(transacao.getContaId(), ignored -> new CopyOnWriteArrayList<>())
                .add(transacao);
    }

    @Override
    public synchronized void salvarTodas(List<Transacao> novasTransacoes) {
        novasTransacoes.forEach(this::salvar);
    }

    @Override
    public List<Transacao> buscarPorConta(long contaId) {
        return transacoes.getOrDefault(contaId, new CopyOnWriteArrayList<>())
                .stream()
                .sorted(Comparator.comparing(Transacao::getHorario).thenComparing(Transacao::getId))
                .toList();
    }
}
