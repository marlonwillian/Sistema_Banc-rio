package br.com.sistemabancario.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class Transacao {
    private static final AtomicLong PROXIMO_ID = new AtomicLong();

    private final Long id;
    private final TipoTransacao tipo;
    private final BigDecimal valor;
    private final LocalDateTime horario;
    private final Long contaId;
    private final Long contaContraparteId;

    public Transacao(
            Long id,
            TipoTransacao tipo,
            BigDecimal valor,
            LocalDateTime horario,
            Long contaId,
            Long contaContraparteId) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("O id da transacao deve ser positivo.");
        }
        this.id = id;
        this.tipo = Objects.requireNonNull(tipo, "O tipo da transacao e obrigatorio.");
        this.valor = validarValor(valor);
        this.horario = Objects.requireNonNull(horario, "O horario da transacao e obrigatorio.");
        if (contaId == null || contaId <= 0) {
            throw new IllegalArgumentException("O id da conta deve ser positivo.");
        }
        if (contaContraparteId != null && contaContraparteId <= 0) {
            throw new IllegalArgumentException("O id da conta contraparte deve ser positivo.");
        }
        this.contaId = contaId;
        this.contaContraparteId = contaContraparteId;
    }

    static Transacao criar(
            TipoTransacao tipo,
            BigDecimal valor,
            LocalDateTime horario,
            long contaId,
            Long contaContraparteId) {
        return new Transacao(
                PROXIMO_ID.incrementAndGet(), tipo, valor, horario, contaId, contaContraparteId);
    }

    private static BigDecimal validarValor(BigDecimal valor) {
        Objects.requireNonNull(valor, "O valor da transacao e obrigatorio.");
        if (valor.signum() <= 0) {
            throw new ValorInvalidoException("O valor da transacao deve ser positivo.");
        }
        try {
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ValorInvalidoException("O valor da transacao nao pode ter mais de duas casas decimais.");
        }
    }

    public Long getId() {
        return id;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDateTime getHorario() {
        return horario;
    }

    public Long getContaId() {
        return contaId;
    }

    public Long getContaContraparteId() {
        return contaContraparteId;
    }
}
