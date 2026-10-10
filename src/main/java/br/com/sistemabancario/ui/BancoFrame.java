package br.com.sistemabancario.ui;

import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.domain.Transacao;
import br.com.sistemabancario.service.BancoService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class BancoFrame extends JFrame {
    private static final Locale LOCALE_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATADOR_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final BancoService bancoService;
    private final JComboBox<Conta> contaOrigem;
    private final JComboBox<Conta> contaDestino;
    private final JTextField campoValor = new JTextField(12);
    private final JLabel saldoAtual = new JLabel();
    private final JTextArea extrato = new JTextArea(14, 64);

    public BancoFrame(BancoService bancoService) {
        super("Sistema Bancario - Operacoes");
        this.bancoService = bancoService;
        List<Conta> contas = bancoService.listarContas();
        ListCellRenderer<Conta> renderizador = criarRenderizadorConta();
        contaOrigem = new JComboBox<>(contas.toArray(Conta[]::new));
        contaDestino = new JComboBox<>(contas.toArray(Conta[]::new));
        contaOrigem.setRenderer(renderizador);
        contaDestino.setRenderer(renderizador);

        configurarJanela();
        add(criarPainelOperacoes(), BorderLayout.NORTH);
        add(criarPainelExtrato(), BorderLayout.CENTER);

        contaOrigem.addActionListener(event -> atualizarContaSelecionada());
        atualizarContaSelecionada();
        pack();
        setLocationRelativeTo(null);
    }

    private void configurarJanela() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(760, 430));
    }

    private JPanel criarPainelOperacoes() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Operacoes bancarias"));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 7, 5, 7);
        constraints.anchor = GridBagConstraints.WEST;

        adicionarCampo(painel, constraints, 0, "Conta:", contaOrigem);
        adicionarCampo(painel, constraints, 1, "Valor (R$):", campoValor);
        adicionarCampo(painel, constraints, 2, "Destino:", contaDestino);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton botaoDepositar = new JButton("Depositar");
        JButton botaoSacar = new JButton("Sacar");
        JButton botaoTransferir = new JButton("Transferir");
        botoes.add(botaoDepositar);
        botoes.add(botaoSacar);
        botoes.add(botaoTransferir);

        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        painel.add(botoes, constraints);
        constraints.gridy = 4;
        saldoAtual.setFont(saldoAtual.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        painel.add(saldoAtual, constraints);

        botaoDepositar.addActionListener(event -> executarOperacao(
                () -> bancoService.depositar(idContaSelecionada(), lerValor())));
        botaoSacar.addActionListener(event -> executarOperacao(
                () -> bancoService.sacar(idContaSelecionada(), lerValor())));
        botaoTransferir.addActionListener(event -> executarOperacao(
                () -> bancoService.transferir(
                        idContaSelecionada(), idContaDestino(), lerValor())));
        return painel;
    }

    private JPanel criarPainelExtrato() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Extrato da conta selecionada"));
        extrato.setEditable(false);
        extrato.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        painel.add(new JScrollPane(extrato), BorderLayout.CENTER);
        return painel;
    }

    private static void adicionarCampo(
            JPanel painel,
            GridBagConstraints constraints,
            int linha,
            String rotulo,
            Component campo) {
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        constraints.gridy = linha;
        painel.add(new JLabel(rotulo), constraints);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        painel.add(campo, constraints);
        constraints.fill = GridBagConstraints.NONE;
    }

    private static ListCellRenderer<Conta> criarRenderizadorConta() {
        return (lista, conta, indice, selecionado, foco) -> {
            DefaultListCellRenderer renderizador = new DefaultListCellRenderer();
            JLabel rotulo = (JLabel) renderizador.getListCellRendererComponent(
                    lista, conta, indice, selecionado, foco);
            if (conta != null) {
                rotulo.setText("Conta " + conta.getId() + " - " + conta.getTitular().nome());
            }
            return rotulo;
        };
    }

    private void executarOperacao(Runnable operacao) {
        try {
            operacao.run();
            campoValor.setText("");
            atualizarContaSelecionada();
            JOptionPane.showMessageDialog(this, "Operacao realizada com sucesso.");
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(
                    this, exception.getMessage(), "Operacao nao realizada", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void atualizarContaSelecionada() {
        Conta conta = contaOrigemSelecionada();
        if (conta == null) {
            saldoAtual.setText("Nenhuma conta selecionada");
            extrato.setText("");
            return;
        }
        saldoAtual.setText("Saldo atual: " + formatarMoeda(conta.getSaldo()));
        extrato.setText(bancoService.buscarExtrato(conta.getId()).stream()
                .map(BancoFrame::formatarTransacao)
                .reduce((primeira, segunda) -> primeira + System.lineSeparator() + segunda)
                .orElse("Nenhuma transacao registrada."));
        extrato.setCaretPosition(0);
    }

    private Conta contaOrigemSelecionada() {
        return (Conta) contaOrigem.getSelectedItem();
    }

    private long idContaSelecionada() {
        Conta conta = contaOrigemSelecionada();
        if (conta == null) {
            throw new IllegalArgumentException("Selecione uma conta.");
        }
        return conta.getId();
    }

    private long idContaDestino() {
        Conta conta = (Conta) contaDestino.getSelectedItem();
        if (conta == null) {
            throw new IllegalArgumentException("Selecione uma conta de destino.");
        }
        return conta.getId();
    }

    private BigDecimal lerValor() {
        String texto = campoValor.getText().trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("Informe o valor da operacao.");
        }
        String normalizado = texto.replace(" ", "");
        if (normalizado.contains(",") && normalizado.contains(".")) {
            normalizado = normalizado.replace(".", "").replace(",", ".");
        } else if (normalizado.contains(",")) {
            normalizado = normalizado.replace(",", ".");
        }
        try {
            return new BigDecimal(normalizado);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Informe um valor valido, por exemplo 25,50.");
        }
    }

    private static String formatarTransacao(Transacao transacao) {
        StringBuilder linha = new StringBuilder()
                .append(transacao.getHorario().format(FORMATADOR_DATA))
                .append(" | ")
                .append(transacao.getTipo())
                .append(" | ")
                .append(formatarMoeda(transacao.getValor()));
        if (transacao.getContaContraparteId() != null) {
            linha.append(" | Conta relacionada: ").append(transacao.getContaContraparteId());
        }
        return linha.toString();
    }

    private static String formatarMoeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(LOCALE_BR).format(valor);
    }
}
