package br.com.sistemabancario.service;

import br.com.sistemabancario.domain.Transacao;
import br.com.sistemabancario.domain.Usuario;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public final class AssistenteIAService {
    private static final String CONFIG_PATH = "/config.properties";
    private static final String DEFAULT_URL = "http://localhost:11434/api/chat";
    private static final String DEFAULT_MODEL = "qwen2.5";
    private static final long DEFAULT_TIMEOUT_SECONDS = 30L;

    private final HttpClient cliente;
    private final Gson gson = new Gson();
    private final BancoService bancoService;

    public AssistenteIAService(BancoService bancoService) {
        this.bancoService = Objects.requireNonNull(bancoService, "O servico bancario e obrigatorio.");
        this.cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String analisarPergunta(String pergunta) {
        String perguntaNormalizada = normalizarPergunta(pergunta);
        Usuario usuario = SessaoUsuario.getUsuarioAtual()
                .orElseThrow(() -> new SecurityException("Usuario nao autenticado para usar o assistente IA."));

        List<Transacao> historico = usuario.eAdmin()
                ? listarTransacoesDeTodasContas()
                : bancoService.buscarExtrato(usuario.contaId());

        if (historico.isEmpty()) {
            return "Não há transações registradas para esta conta ou para o banco no momento.";
        }

        List<Transacao> contexto = historico.size() <= 25 ? historico : historico.subList(0, 25);
        String prompt = montarPrompt(perguntaNormalizada, contexto);
        return consultarOllama(prompt);
    }

    public String resumoAutomatico() {
        Usuario usuario = SessaoUsuario.getUsuarioAtual()
                .orElseThrow(() -> new SecurityException("Usuario nao autenticado para usar o assistente IA."));
        List<Transacao> historico = usuario.eAdmin()
                ? listarTransacoesDeTodasContas()
                : bancoService.buscarExtrato(usuario.contaId());

        if (historico.isEmpty()) {
            return "Nenhuma transação disponível para análise automática.";
        }

        BigDecimal totalEntradas = historico.stream()
                .filter(transacao -> transacao.getTipo().name().contains("DEPOSITO")
                        || transacao.getTipo().name().contains("RECEBIDA"))
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSaidas = historico.stream()
                .filter(transacao -> transacao.getTipo().name().contains("SAQUE")
                        || transacao.getTipo().name().contains("ENVIADA"))
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal maiorValor = historico.stream()
                .map(Transacao::getValor)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        double media = historico.stream()
                .map(Transacao::getValor)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0d);

        List<String> alertas = new ArrayList<>();
        for (Transacao transacao : historico) {
            if (transacao.getValor().doubleValue() > media * 2.5) {
                alertas.add("Valor acima da média em " + transacao.getHorario() + ": " + transacao.getValor());
            }
        }

        return "Resumo automático:\n"
                + "- Entradas totais: " + totalEntradas + "\n"
                + "- Saídas totais: " + totalSaidas + "\n"
                + "- Maior movimentação: " + maiorValor + "\n"
                + "- Média de movimentações: " + String.format("%.2f", media) + "\n"
                + (alertas.isEmpty() ? "- Sem alertas relevantes." : "- Alertas: " + String.join("; ", alertas));
    }

    private String consultarOllama(String prompt) {
        String url = configuracao("sistema.ia.url", DEFAULT_URL);
        String modelo = configuracao("sistema.ia.modelo", DEFAULT_MODEL);
        long timeoutEmSegundos = Long.parseLong(configuracao("sistema.ia.timeoutSegundos", String.valueOf(DEFAULT_TIMEOUT_SECONDS)));

        JsonObject payload = new JsonObject();
        payload.addProperty("model", modelo);
        payload.addProperty("stream", false);
        JsonArray messages = new JsonArray();

        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content",
                "Você é um analista financeiro do banco. Responda em português, use apenas os dados fornecidos e não invente valores.");

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prompt);

        messages.add(system);
        messages.add(user);
        payload.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeoutEmSegundos))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        try {
            HttpResponse<String> response = cliente.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("O assistente IA não respondeu corretamente: " + response.body());
            }

            JsonObject resposta = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonElement conteudo = resposta.getAsJsonObject("message").get("content");
            return conteudo == null ? "Sem resposta da IA." : conteudo.getAsString();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A consulta ao Ollama foi interrompida.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Não foi possível conectar ao Ollama. Inicie o serviço com `ollama serve` "
                            + "e confirme que o modelo qwen2.5 foi instalado com `ollama pull qwen2.5`.",
                    exception);
        }
    }

    private String montarPrompt(String pergunta, List<Transacao> historico) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Dados financeiros disponíveis:\n");
        for (Transacao transacao : historico) {
            prompt.append("- dataHora=")
                    .append(transacao.getHorario())
                    .append(", tipo=")
                    .append(transacao.getTipo())
                    .append(", valor=")
                    .append(transacao.getValor())
                    .append(", contaId=")
                    .append(transacao.getContaId())
                    .append(", contaContraparteId=")
                    .append(transacao.getContaContraparteId() == null ? "-" : transacao.getContaContraparteId())
                    .append("\n");
        }
        prompt.append("\nPergunta do usuário: ")
                .append(pergunta)
                .append("\n");
        return prompt.toString();
    }

    private List<Transacao> listarTransacoesDeTodasContas() {
        List<Transacao> todas = new ArrayList<>();
        for (var conta : bancoService.listarContas()) {
            todas.addAll(bancoService.buscarExtrato(conta.getId()));
        }
        todas.sort(Comparator.comparing(Transacao::getHorario));
        return todas;
    }

    private static String normalizarPergunta(String pergunta) {
        if (pergunta == null) {
            throw new IllegalArgumentException("Informe a pergunta para a IA.");
        }
        String valor = pergunta.trim();
        if (valor.isBlank()) {
            throw new IllegalArgumentException("A pergunta não pode ficar vazia.");
        }
        return valor;
    }

    private static String configuracao(String chave, String padrao) {
        Properties propriedades = new Properties();
        try (InputStream entrada = AssistenteIAService.class.getResourceAsStream(CONFIG_PATH)) {
            if (entrada != null) {
                propriedades.load(new java.io.InputStreamReader(entrada, StandardCharsets.UTF_8));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível ler as configurações do assistente IA.", exception);
        }

        String valor = System.getProperty(chave);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(chave.replace('.', '_').toUpperCase());
        }
        if (valor == null || valor.isBlank()) {
            valor = propriedades.getProperty(chave, padrao);
        }
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}
