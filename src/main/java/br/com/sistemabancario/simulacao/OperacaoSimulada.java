package br.com.sistemabancario.simulacao;

import java.math.BigDecimal;
import java.util.Objects;

public record OperacaoSimulada(
        TipoOperacao tipo,
        long contaOrigemId,
        Long contaDestinoId,
        BigDecimal valor) {
    public OperacaoSimulada {
        Objects.requireNonNull(tipo, "O tipo da operacao e obrigatorio.");
        Objects.requireNonNull(valor, "O valor da operacao e obrigatorio.");
        if (tipo == TipoOperacao.TRANSFERENCIA && contaDestinoId == null) {
            throw new IllegalArgumentException("Transferencia requer uma conta de destino.");
        }
        if (tipo != TipoOperacao.TRANSFERENCIA && contaDestinoId != null) {
            throw new IllegalArgumentException("Somente transferencia possui conta de destino.");
        }
    }
}
