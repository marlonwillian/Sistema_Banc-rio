package br.com.sistemabancario.persistence;

import br.com.sistemabancario.domain.Transacao;

import java.util.List;

public interface TransacaoRepository {
    void salvar(Transacao transacao);

    List<Transacao> buscarPorConta(long contaId);
}
