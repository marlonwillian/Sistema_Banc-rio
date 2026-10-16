package br.com.sistemabancario.io;

import br.com.sistemabancario.domain.Transacao;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class ExtratoExporter {
    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Locale LOCALE_BR = Locale.forLanguageTag("pt-BR");

    public void exportar(long contaId, String titular, List<Transacao> transacoes, Path destino) {
        try (BufferedWriter writer = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            writer.write("EXTRATO BANCARIO");
            writer.newLine();
            writer.write("Conta: " + contaId + " | Titular: " + titular);
            writer.newLine();
            writer.write("------------------------------------------------------------");
            writer.newLine();
            if (transacoes.isEmpty()) {
                writer.write("Nenhuma transacao registrada.");
                writer.newLine();
            } else {
                for (Transacao transacao : transacoes) {
                    writer.write(formatar(transacao));
                    writer.newLine();
                }
            }
        } catch (IOException exception) {
            throw new ExportacaoException("Nao foi possivel exportar o extrato para " + destino + ".", exception);
        }
    }

    private static String formatar(Transacao transacao) {
        String relacionada = transacao.getContaContraparteId() == null
                ? ""
                : " | Conta relacionada: " + transacao.getContaContraparteId();
        return transacao.getHorario().format(FORMATO_DATA)
                + " | " + transacao.getTipo()
                + " | " + NumberFormat.getCurrencyInstance(LOCALE_BR).format(transacao.getValor())
                + relacionada;
    }

    public static String formatarValor(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(LOCALE_BR).format(valor);
    }
}
