package br.com.sistemabancario.simulacao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ResultadoSimulacao(
        int operacoesPlanejadas,
        int operacoesRealizadas,
        int operacoesRecusadasPorSaldo,
        Map<Long, BigDecimal> saldosEsperados,
        Map<Long, BigDecimal> saldosObtidos,
        List<String> falhas) {
    public ResultadoSimulacao {
        saldosEsperados = Map.copyOf(saldosEsperados);
        saldosObtidos = Map.copyOf(saldosObtidos);
        falhas = List.copyOf(falhas);
    }

    public boolean saldosCorretos() {
        return saldosEsperados.equals(saldosObtidos);
    }
}
