package br.com.sistemabancario.persistence;

import br.com.sistemabancario.domain.Transacao;

import java.util.List;

public interface TransacaoRepository {
    void salvar(Transacao transacao);

    default void salvarTodas(List<Transacao> transacoes) {
        transacoes.forEach(this::salvar);
    }

    default boolean persisteSaldosJuntoComTransacoes() {
        return false;
    }

    List<Transacao> buscarPorConta(long contaId);
}
