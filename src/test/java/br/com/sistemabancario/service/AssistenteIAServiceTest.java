package br.com.sistemabancario.service;

import br.com.sistemabancario.domain.Cliente;
import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.domain.Perfil;
import br.com.sistemabancario.domain.Usuario;
import br.com.sistemabancario.persistence.memory.InMemoryTransacaoRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AssistenteIAServiceTest {
    @Test
    void consultaOllamaSemStreamingELeResposta() throws IOException {
        AtomicReference<JsonObject> payloadRecebido = new AtomicReference<>();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/chat", exchange -> {
            payloadRecebido.set(JsonParser.parseString(
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject());
            byte[] resposta = "{\"message\":{\"content\":\"Total movimentado: R$ 12,34\"}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resposta.length);
            try (var corpo = exchange.getResponseBody()) {
                corpo.write(resposta);
            }
        });

        String urlOriginal = System.getProperty("sistema.ia.url");
        String modeloOriginal = System.getProperty("sistema.ia.modelo");
        String timeoutOriginal = System.getProperty("sistema.ia.timeoutSegundos");
        servidor.start();
        try {
            System.setProperty(
                    "sistema.ia.url",
                    "http://127.0.0.1:" + servidor.getAddress().getPort() + "/api/chat");
            System.setProperty("sistema.ia.modelo", "qwen-test");
            System.setProperty("sistema.ia.timeoutSegundos", "5");

            Conta conta = new Conta(1, new Cliente(1L, "Ana"), BigDecimal.ZERO);
            InMemoryTransacaoRepository transacoes = new InMemoryTransacaoRepository();
            transacoes.salvar(conta.depositar(new BigDecimal("12.34")));
            BancoService bancoService = new BancoService(List.of(conta), transacoes);
            SessaoUsuario.abrir(new Usuario(1L, "admin", "hash", Perfil.ADMIN, null));

            String resposta = new AssistenteIAService(bancoService)
                    .analisarPergunta("Quanto movimentei?");

            assertEquals("Total movimentado: R$ 12,34", resposta);
            assertFalse(payloadRecebido.get().get("stream").getAsBoolean());
            assertEquals("qwen-test", payloadRecebido.get().get("model").getAsString());
        } finally {
            restaurarPropriedade("sistema.ia.url", urlOriginal);
            restaurarPropriedade("sistema.ia.modelo", modeloOriginal);
            restaurarPropriedade("sistema.ia.timeoutSegundos", timeoutOriginal);
            SessaoUsuario.fechar();
            servidor.stop(0);
        }
    }

    private static void restaurarPropriedade(String nome, String valorOriginal) {
        if (valorOriginal == null) {
            System.clearProperty(nome);
        } else {
            System.setProperty(nome, valorOriginal);
        }
    }
}
