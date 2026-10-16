package br.com.sistemabancario.io;

import br.com.sistemabancario.domain.TipoTransacao;
import br.com.sistemabancario.domain.Transacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtratoExporterTest {
    @Test
    void exportaExtratoEmUtf8ComDadosDaConta(@TempDir Path diretorio) throws Exception {
        Path destino = diretorio.resolve("extrato.txt");
        Transacao transacao = new Transacao(
                1L,
                TipoTransacao.DEPOSITO,
                new BigDecimal("25.50"),
                LocalDateTime.of(2026, 10, 16, 19, 0),
                1L,
                null);

        new ExtratoExporter().exportar(1, "Ana Silva", List.of(transacao), destino);

        String conteudo = Files.readString(destino, StandardCharsets.UTF_8);
        assertTrue(conteudo.contains("Titular: Ana Silva"));
        assertTrue(conteudo.contains("DEPOSITO"));
        assertTrue(conteudo.contains("R$"));
        assertTrue(conteudo.contains("25,50"));
    }
}
