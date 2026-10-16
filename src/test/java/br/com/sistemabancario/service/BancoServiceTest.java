package br.com.sistemabancario.service;

import br.com.sistemabancario.domain.Cliente;
import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.domain.SaldoInsuficienteException;
import br.com.sistemabancario.domain.TipoTransacao;
import br.com.sistemabancario.persistence.memory.InMemoryContaRepository;
import br.com.sistemabancario.persistence.memory.InMemoryTransacaoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BancoServiceTest {
    @Test
    void depositoAtualizaContaEExtratoConsultadoPeloServico() {
        Conta conta = new Conta(1, new Cliente(1L, "Ana"), BigDecimal.ZERO);
        BancoService service = new BancoService(
                List.of(conta), new InMemoryTransacaoRepository());

        service.depositar(1, new BigDecimal("25.50"));

        assertEquals(new BigDecimal("25.50"),
                service.buscarContaPorId(1).orElseThrow().getSaldo());
        assertEquals(1, service.buscarExtrato(1).size());
    }

    @Test
    void transferenciaRegistraSaidaEEntradaNosExtratosDasContas() {
        Conta origem = new Conta(1, new Cliente(1L, "Ana"), new BigDecimal("40.00"));
        Conta destino = new Conta(2, new Cliente(2L, "Bruno"), BigDecimal.ZERO);
        BancoService service = new BancoService(
                List.of(origem, destino), new InMemoryTransacaoRepository());

        service.transferir(1, 2, new BigDecimal("12.00"));

        assertEquals(TipoTransacao.TRANSFERENCIA_ENVIADA,
                service.buscarExtrato(1).get(0).getTipo());
        assertEquals(TipoTransacao.TRANSFERENCIA_RECEBIDA,
                service.buscarExtrato(2).get(0).getTipo());
    }

    @Test
    void saqueRecusadoNaoEAdicionadoAoExtrato() {
        Conta conta = new Conta(1, new Cliente(1L, "Ana"), BigDecimal.ZERO);
        BancoService service = new BancoService(
                List.of(conta), new InMemoryTransacaoRepository());

        assertThrows(SaldoInsuficienteException.class,
                () -> service.sacar(1, BigDecimal.ONE));
        assertEquals(0, service.buscarExtrato(1).size());
    }

    @Test
    void repositoriosEmMemoriaPodemSerReabertosSemPerderEstadoDaContaOuExtrato() {
        InMemoryContaRepository contas = new InMemoryContaRepository();
        InMemoryTransacaoRepository transacoes = new InMemoryTransacaoRepository();
        contas.salvar(new Conta(1, new Cliente(1L, "Ana"), BigDecimal.ZERO));
        BancoService service = new BancoService(contas, transacoes);

        service.depositar(1, new BigDecimal("18.75"));
        BancoService recarregado = new BancoService(contas, transacoes);

        assertEquals(new BigDecimal("18.75"),
                recarregado.buscarContaPorId(1).orElseThrow().getSaldo());
        assertEquals(1, recarregado.buscarExtrato(1).size());
    }
}
