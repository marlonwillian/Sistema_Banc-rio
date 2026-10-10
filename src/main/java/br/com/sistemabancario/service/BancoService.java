package br.com.sistemabancario.service;

import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.domain.Transacao;
import br.com.sistemabancario.persistence.TransacaoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class BancoService {
    private final Map<Long, Conta> contas;
    private final TransacaoRepository transacaoRepository;

    public BancoService(List<Conta> contas, TransacaoRepository transacaoRepository) {
        this.contas = contas.stream().collect(Collectors.toUnmodifiableMap(
                Conta::getId, Function.identity()));
        this.transacaoRepository = transacaoRepository;
    }

    public List<Conta> listarContas() {
        return contas.values().stream()
                .sorted((primeira, segunda) -> Long.compare(primeira.getId(), segunda.getId()))
                .toList();
    }

    public Transacao depositar(long contaId, BigDecimal valor) {
        Transacao transacao = buscarConta(contaId).depositar(valor);
        transacaoRepository.salvar(transacao);
        return transacao;
    }

    public Transacao sacar(long contaId, BigDecimal valor) {
        Transacao transacao = buscarConta(contaId).sacar(valor);
        transacaoRepository.salvar(transacao);
        return transacao;
    }

    public List<Transacao> transferir(long origemId, long destinoId, BigDecimal valor) {
        List<Transacao> transacoes = buscarConta(origemId)
                .transferirPara(buscarConta(destinoId), valor);
        transacoes.forEach(transacaoRepository::salvar);
        return transacoes;
    }

    public List<Transacao> buscarExtrato(long contaId) {
        buscarConta(contaId);
        return transacaoRepository.buscarPorConta(contaId);
    }

    private Conta buscarConta(long contaId) {
        Conta conta = contas.get(contaId);
        if (conta == null) {
            throw new IllegalArgumentException("Conta " + contaId + " nao encontrada.");
        }
        return conta;
    }
}
