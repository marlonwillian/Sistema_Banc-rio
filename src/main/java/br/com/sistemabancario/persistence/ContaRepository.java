package br.com.sistemabancario.persistence;

import br.com.sistemabancario.domain.Conta;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ContaRepository {
    void salvar(Conta conta);

    Optional<Conta> buscarPorId(long id);

    List<Conta> listarTodas();

    void atualizarSaldo(long contaId, BigDecimal novoSaldo);
}
