package br.com.sistemabancario.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Conta {
    private final long id;
    private final Cliente titular;
    private BigDecimal saldo;
    private final List<Transacao> extrato = new ArrayList<>();

    public Conta(long id, Cliente titular, BigDecimal saldoInicial) {
        if (id <= 0) {
            throw new IllegalArgumentException("O id da conta deve ser positivo.");
        }
        this.id = id;
        this.titular = Objects.requireNonNull(titular, "O titular da conta e obrigatorio.");
        this.saldo = validarSaldoInicial(saldoInicial);
    }

    public long getId() {
        return id;
    }

    public Cliente getTitular() {
        return titular;
    }

    public synchronized BigDecimal getSaldo() {
        return saldo;
    }

    public synchronized List<Transacao> getExtrato() {
        return List.copyOf(extrato);
    }

    public synchronized Transacao depositar(BigDecimal valor) {
        BigDecimal valorValidado = validarValorOperacao(valor);
        LocalDateTime horario = LocalDateTime.now();
        Transacao transacao = Transacao.criar(
                TipoTransacao.DEPOSITO, valorValidado, horario, id, null);
        saldo = saldo.add(valorValidado);
        extrato.add(transacao);
        return transacao;
    }

    public synchronized Transacao sacar(BigDecimal valor) {
        BigDecimal valorValidado = validarValorOperacao(valor);
        validarSaldoSuficiente(valorValidado);
        LocalDateTime horario = LocalDateTime.now();
        Transacao transacao = Transacao.criar(
                TipoTransacao.SAQUE, valorValidado, horario, id, null);
        saldo = saldo.subtract(valorValidado);
        extrato.add(transacao);
        return transacao;
    }

    public List<Transacao> transferirPara(Conta destino, BigDecimal valor) {
        Objects.requireNonNull(destino, "A conta de destino e obrigatoria.");
        if (destino == this || destino.id == id) {
            throw new OperacaoInvalidaException(
                    "A conta de destino deve ter um id diferente da conta de origem.");
        }
        BigDecimal valorValidado = validarValorOperacao(valor);

        Conta primeira = id < destino.id ? this : destino;
        Conta segunda = id < destino.id ? destino : this;
        synchronized (primeira) {
            synchronized (segunda) {
                validarSaldoSuficiente(valorValidado);
                LocalDateTime horario = LocalDateTime.now();
                Transacao enviada = Transacao.criar(
                        TipoTransacao.TRANSFERENCIA_ENVIADA,
                        valorValidado,
                        horario,
                        id,
                        destino.id);
                Transacao recebida = Transacao.criar(
                        TipoTransacao.TRANSFERENCIA_RECEBIDA,
                        valorValidado,
                        horario,
                        destino.id,
                        id);

                saldo = saldo.subtract(valorValidado);
                destino.saldo = destino.saldo.add(valorValidado);
                extrato.add(enviada);
                destino.extrato.add(recebida);
                return List.of(enviada, recebida);
            }
        }
    }

    private void validarSaldoSuficiente(BigDecimal valor) {
        if (saldo.compareTo(valor) < 0) {
            throw new SaldoInsuficienteException("Saldo insuficiente na conta " + id + ".");
        }
    }

    private static BigDecimal validarSaldoInicial(BigDecimal saldoInicial) {
        Objects.requireNonNull(saldoInicial, "O saldo inicial e obrigatorio.");
        if (saldoInicial.signum() < 0) {
            throw new ValorInvalidoException("O saldo inicial nao pode ser negativo.");
        }
        return normalizarValor(saldoInicial);
    }

    private static BigDecimal validarValorOperacao(BigDecimal valor) {
        Objects.requireNonNull(valor, "O valor da operacao e obrigatorio.");
        if (valor.signum() <= 0) {
            throw new ValorInvalidoException("O valor da operacao deve ser positivo.");
        }
        return normalizarValor(valor);
    }

    private static BigDecimal normalizarValor(BigDecimal valor) {
        try {
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ValorInvalidoException("O valor nao pode ter mais de duas casas decimais.");
        }
    }
}
