package br.com.sistemabancario.simulacao;

import br.com.sistemabancario.domain.Cliente;
import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.persistence.memory.InMemoryTransacaoRepository;
import br.com.sistemabancario.service.BancoService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulacaoRunnerTest {
    @Test
    void simulacaoConcorrenteComparaSaldoEsperadoEObtido() {
        BancoService service = new BancoService(
                List.of(
                        new Conta(1, new Cliente(1L, "Ana"), new BigDecimal("100.00")),
                        new Conta(2, new Cliente(2L, "Bruno"), new BigDecimal("100.00"))),
                new InMemoryTransacaoRepository());

        ResultadoSimulacao resultado =
                new SimulacaoRunner(service).executar(8, 100, new BigDecimal("1.00"));

        assertEquals(800, resultado.operacoesPlanejadas());
        assertEquals(800, resultado.operacoesRealizadas());
        assertEquals(0, resultado.operacoesRecusadasPorSaldo());
        assertTrue(resultado.saldosCorretos());
        assertEquals(new BigDecimal("200.00"),
                resultado.saldosEsperados().get(1L).add(resultado.saldosEsperados().get(2L)));
    }

    @Test
    void operacoesRecusadasPorSaldoSaoContabilizadasSemDerrubarSimulacao() {
        BancoService service = new BancoService(
                List.of(
                        new Conta(1, new Cliente(1L, "Ana"), new BigDecimal("5.00")),
                        new Conta(2, new Cliente(2L, "Bruno"), BigDecimal.ZERO)),
                new InMemoryTransacaoRepository());
        List<OperacaoSimulada> operacoes = List.of(
                new OperacaoSimulada(TipoOperacao.SAQUE, 1, null, new BigDecimal("10.00")),
                new OperacaoSimulada(TipoOperacao.DEPOSITO, 2, null, new BigDecimal("2.00")));

        ResultadoSimulacao resultado =
                new SimulacaoRunner(service).executar(operacoes, 2);

        assertEquals(1, resultado.operacoesRealizadas());
        assertEquals(1, resultado.operacoesRecusadasPorSaldo());
        assertTrue(resultado.saldosCorretos());
        assertEquals(new BigDecimal("5.00"), resultado.saldosObtidos().get(1L));
        assertEquals(new BigDecimal("2.00"), resultado.saldosObtidos().get(2L));
    }
}
