package br.com.sistemabancario.ui;

import br.com.sistemabancario.domain.Cliente;
import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.domain.TipoTransacao;
import br.com.sistemabancario.domain.Transacao;
import br.com.sistemabancario.io.ExtratoExporter;
import br.com.sistemabancario.simulacao.ResultadoSimulacao;
import br.com.sistemabancario.simulacao.SimulacaoRunner;
import br.com.sistemabancario.simulacao.demo.DemonstracaoRaceCondition;
import br.com.sistemabancario.service.BancoService;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class BancoFrame extends JFrame {
    private static final Locale LOCALE_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATADOR_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");
    private static final Color FUNDO = new Color(0xF4F6F8);
    private static final Color BRANCO = Color.WHITE;
    private static final Color MARINHO = new Color(0x18212F);
    private static final Color TEXTO = new Color(0x202B3C);
    private static final Color TEXTO_SECUNDARIO = new Color(0x7C8796);
    private static final Color TURQUESA = new Color(0x16AAA5);
    private static final Color LINHA = new Color(0xE9EDF1);

    private final BancoService bancoService;
    private final JComboBox<Conta> contaOrigem;
    private final JComboBox<Conta> contaDestino;
    private final JTextField campoValor = new JTextField(18);
    private final JLabel saldoAtual = new JLabel();
    private final JLabel nomeTitular = new JLabel();
    private final JLabel nomeTitularResumo = new JLabel();
    private final JLabel totalTransacoes = new JLabel("0");
    private final DefaultTableModel modeloExtrato = new DefaultTableModel(
            new Object[]{"DATA", "OPERAÇÃO", "CONTA RELACIONADA", "VALOR"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JPanel painelOperacoes = new JPanel();
    private final JPanel painelExtrato = new JPanel();

    public BancoFrame(BancoService bancoService) {
        super("Sistema Bancário");
        this.bancoService = bancoService;
        List<Conta> contas = bancoService.listarContas();
        ListCellRenderer<Conta> renderizador = criarRenderizadorConta();
        contaOrigem = new JComboBox<>(contas.toArray(Conta[]::new));
        contaDestino = new JComboBox<>(contas.toArray(Conta[]::new));
        contaOrigem.setRenderer(renderizador);
        contaDestino.setRenderer(renderizador);

        configurarEstilo();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 680));
        setPreferredSize(new Dimension(1160, 790));
        setContentPane(criarConteudo());
        contaOrigem.addActionListener(event -> atualizarPainel());
        atualizarPainel();
        pack();
        setLocationRelativeTo(null);
    }

    public static void instalarTema() {
        FlatLightLaf.setup();
        configurarDefaults();
    }

    private static void configurarDefaults() {
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.selectionBackground", new Color(0xE4F5F3));
        UIManager.put("Table.selectionForeground", TEXTO);
    }

    private void configurarEstilo() {
        getContentPane().setBackground(FUNDO);
    }

    private JPanel criarConteudo() {
        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(FUNDO);
        raiz.add(criarBarraLateral(), BorderLayout.WEST);

        JPanel pagina = new JPanel(new BorderLayout());
        pagina.setOpaque(false);
        pagina.setBorder(BorderFactory.createEmptyBorder(28, 34, 28, 34));
        pagina.add(criarCabecalho(), BorderLayout.NORTH);
        pagina.add(criarCorpo(), BorderLayout.CENTER);
        raiz.add(pagina, BorderLayout.CENTER);
        return raiz;
    }

    private JPanel criarBarraLateral() {
        JPanel barra = new JPanel();
        barra.setBackground(MARINHO);
        barra.setPreferredSize(new Dimension(226, 0));
        barra.setLayout(new javax.swing.BoxLayout(barra, javax.swing.BoxLayout.Y_AXIS));
        barra.setBorder(BorderFactory.createEmptyBorder(28, 18, 22, 18));

        JPanel marca = new JPanel(new FlowLayout(FlowLayout.LEFT, 11, 0));
        marca.setOpaque(false);
        JLabel simbolo = new JLabel("B");
        simbolo.setOpaque(true);
        simbolo.setBackground(TURQUESA);
        simbolo.setForeground(Color.WHITE);
        simbolo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        simbolo.setHorizontalAlignment(SwingConstants.CENTER);
        simbolo.setPreferredSize(new Dimension(36, 36));
        JLabel nome = new JLabel("<html><b>Sistema</b><br>Bancário</html>");
        nome.setForeground(Color.WHITE);
        nome.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        marca.add(simbolo);
        marca.add(nome);
        marca.setAlignmentX(Component.LEFT_ALIGNMENT);
        barra.add(marca);
        barra.add(javax.swing.Box.createVerticalStrut(42));

        JLabel menu = new JLabel("MENU PRINCIPAL");
        menu.setForeground(new Color(0x99A5B4));
        menu.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        menu.setAlignmentX(Component.LEFT_ALIGNMENT);
        barra.add(menu);
        barra.add(javax.swing.Box.createVerticalStrut(12));

        JButton visaoGeral = criarBotaoLateral("▦   Visão geral", true);
        visaoGeral.setAlignmentX(Component.LEFT_ALIGNMENT);
        barra.add(visaoGeral);
        barra.add(javax.swing.Box.createVerticalStrut(7));
        JButton operacoes = criarBotaoLateral("↔   Operações", false);
        operacoes.setAlignmentX(Component.LEFT_ALIGNMENT);
        barra.add(operacoes);
        barra.add(javax.swing.Box.createVerticalStrut(7));
        JButton extrato = criarBotaoLateral("≡   Extrato", false);
        extrato.setAlignmentX(Component.LEFT_ALIGNMENT);
        barra.add(extrato);
        JButton demonstracao = criarBotaoLateral("⚡   Race condition", false);
        demonstracao.setAlignmentX(Component.LEFT_ALIGNMENT);
        barra.add(javax.swing.Box.createVerticalStrut(7));
        barra.add(demonstracao);

        operacoes.addActionListener(event -> painelOperacoes.scrollRectToVisible(
                new Rectangle(0, 0, painelOperacoes.getWidth(), painelOperacoes.getHeight())));
        extrato.addActionListener(event -> painelExtrato.scrollRectToVisible(
                new Rectangle(0, 0, painelExtrato.getWidth(), painelExtrato.getHeight())));
        demonstracao.addActionListener(event -> demonstrarRaceCondition());

        barra.add(javax.swing.Box.createVerticalGlue());
        JLabel rodape = new JLabel("<html>AMBIENTE DE DEMONSTRAÇÃO<br><br>Operações registradas na sessão</html>");
        rodape.setForeground(new Color(0x99A5B4));
        rodape.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        rodape.setAlignmentX(Component.LEFT_ALIGNMENT);
        barra.add(rodape);
        return barra;
    }

    private static JButton criarBotaoLateral(String texto, boolean selecionado) {
        JButton botao = new JButton(texto);
        botao.setHorizontalAlignment(SwingConstants.LEFT);
        botao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        botao.setPreferredSize(new Dimension(190, 42));
        botao.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        botao.setFocusPainted(false);
        botao.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 10));
        botao.setForeground(selecionado ? Color.WHITE : new Color(0xC5CDD7));
        botao.setBackground(selecionado ? new Color(0x273747) : MARINHO);
        botao.setOpaque(true);
        return botao;
    }

    private JPanel criarCabecalho() {
        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setOpaque(false);
        cabecalho.setBorder(BorderFactory.createEmptyBorder(0, 0, 23, 0));

        JPanel titulos = new JPanel();
        titulos.setOpaque(false);
        titulos.setLayout(new javax.swing.BoxLayout(titulos, javax.swing.BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("Visão geral");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 25));
        titulo.setForeground(TEXTO);
        JLabel subtitulo = new JLabel("Acompanhe sua conta e realize operações bancárias.");
        subtitulo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        subtitulo.setForeground(TEXTO_SECUNDARIO);
        titulos.add(titulo);
        titulos.add(javax.swing.Box.createVerticalStrut(5));
        titulos.add(subtitulo);
        cabecalho.add(titulos, BorderLayout.WEST);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        acoes.setOpaque(false);
        JButton novaConta = criarBotao("＋ Nova conta", true);
        JLabel ambiente = new JLabel("  ●  Banco local");
        ambiente.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        ambiente.setForeground(new Color(0x308B70));
        ambiente.setOpaque(true);
        ambiente.setBackground(new Color(0xE5F4EF));
        ambiente.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 12));
        acoes.add(novaConta);
        acoes.add(ambiente);
        novaConta.addActionListener(event -> criarConta());
        cabecalho.add(acoes, BorderLayout.EAST);
        return cabecalho;
    }

    private JScrollPane criarCorpo() {
        JPanel corpo = new JPanel();
        corpo.setOpaque(false);
        corpo.setLayout(new javax.swing.BoxLayout(corpo, javax.swing.BoxLayout.Y_AXIS));
        corpo.add(criarCartaoSaldo());
        corpo.add(javax.swing.Box.createVerticalStrut(16));
        corpo.add(criarCartoesResumo());
        corpo.add(javax.swing.Box.createVerticalStrut(17));
        corpo.add(criarPainelOperacoes());
        corpo.add(javax.swing.Box.createVerticalStrut(17));
        corpo.add(criarPainelExtrato());

        JScrollPane scroll = new JScrollPane(corpo);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private JPanel criarCartaoSaldo() {
        JPanel cartao = new Cartao(new Color(0x163D4A), 16);
        cartao.setLayout(new BorderLayout(20, 0));
        cartao.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        cartao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 166));

        JPanel informacao = new JPanel();
        informacao.setOpaque(false);
        informacao.setLayout(new javax.swing.BoxLayout(informacao, javax.swing.BoxLayout.Y_AXIS));
        JLabel rotulo = new JLabel("SALDO DISPONÍVEL");
        rotulo.setForeground(new Color(0xB6D1D5));
        rotulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        saldoAtual.setForeground(Color.WHITE);
        saldoAtual.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 29));
        nomeTitular.setForeground(new Color(0xD4E4E6));
        nomeTitular.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        informacao.add(rotulo);
        informacao.add(javax.swing.Box.createVerticalStrut(8));
        informacao.add(saldoAtual);
        informacao.add(javax.swing.Box.createVerticalStrut(4));
        informacao.add(nomeTitular);
        cartao.add(informacao, BorderLayout.CENTER);

        JPanel seletor = new JPanel();
        seletor.setOpaque(false);
        seletor.setLayout(new javax.swing.BoxLayout(seletor, javax.swing.BoxLayout.Y_AXIS));
        JLabel trocarConta = new JLabel("CONTA SELECIONADA");
        trocarConta.setForeground(new Color(0xB6D1D5));
        trocarConta.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        contaOrigem.setPreferredSize(new Dimension(230, 38));
        contaOrigem.setMaximumSize(new Dimension(250, 38));
        seletor.add(trocarConta);
        seletor.add(javax.swing.Box.createVerticalStrut(9));
        seletor.add(contaOrigem);
        cartao.add(seletor, BorderLayout.EAST);
        return cartao;
    }

    private JPanel criarCartoesResumo() {
        JPanel linha = new JPanel(new GridBagLayout());
        linha.setOpaque(false);
        linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 0, 10);
        constraints.gridx = 0;
        linha.add(criarCartaoResumo("TITULAR DA CONTA", nomeTitularResumo), constraints);
        constraints.gridx = 1;
        constraints.insets = new Insets(0, 0, 0, 0);
        linha.add(criarCartaoResumo("TRANSAÇÕES REGISTRADAS", totalTransacoes), constraints);
        return linha;
    }

    private static JPanel criarCartaoResumo(String rotulo, JLabel valor) {
        JPanel cartao = new Cartao(BRANCO, 12);
        cartao.setLayout(new javax.swing.BoxLayout(cartao, javax.swing.BoxLayout.Y_AXIS));
        cartao.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        JLabel titulo = new JLabel(rotulo);
        titulo.setForeground(TEXTO_SECUNDARIO);
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        valor.setForeground(TEXTO);
        valor.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        cartao.add(titulo);
        cartao.add(javax.swing.Box.createVerticalStrut(8));
        cartao.add(valor);
        return cartao;
    }

    private JPanel criarPainelOperacoes() {
        painelOperacoes.setOpaque(true);
        painelOperacoes.setBackground(BRANCO);
        painelOperacoes.setLayout(new BorderLayout());
        painelOperacoes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINHA),
                BorderFactory.createEmptyBorder(19, 22, 18, 22)));
        painelOperacoes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);
        JLabel titulo = new JLabel("Nova operação");
        titulo.setForeground(TEXTO);
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        JLabel dica = new JLabel("Preencha os dados para registrar uma movimentação.");
        dica.setForeground(TEXTO_SECUNDARIO);
        dica.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new javax.swing.BoxLayout(textos, javax.swing.BoxLayout.Y_AXIS));
        textos.add(titulo);
        textos.add(javax.swing.Box.createVerticalStrut(4));
        textos.add(dica);
        topo.add(textos, BorderLayout.WEST);
        painelOperacoes.add(topo, BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);
        GridBagConstraints campo = new GridBagConstraints();
        campo.insets = new Insets(15, 0, 0, 12);
        campo.anchor = GridBagConstraints.WEST;
        campo.fill = GridBagConstraints.HORIZONTAL;
        campo.weightx = 1;
        adicionarCampo(formulario, campo, 0, 0, "VALOR (R$)", campoValor);
        adicionarCampo(formulario, campo, 1, 0, "CONTA DE DESTINO", contaDestino);
        painelOperacoes.add(formulario, BorderLayout.CENTER);

        JPanel rodape = new JPanel(new BorderLayout(12, 0));
        rodape.setOpaque(false);
        rodape.setBorder(BorderFactory.createEmptyBorder(13, 0, 0, 0));
        JLabel aviso = new JLabel("Os saques e transferências validam o saldo disponível.");
        aviso.setForeground(TEXTO_SECUNDARIO);
        aviso.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        rodape.add(aviso, BorderLayout.WEST);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);
        JButton depositar = criarBotao("Depositar", false);
        JButton sacar = criarBotao("Sacar", false);
        JButton transferir = criarBotao("Transferir", true);
        JButton simular = criarBotao("Simular", false);
        JButton exportar = criarBotao("Exportar extrato", false);
        botoes.add(depositar);
        botoes.add(sacar);
        botoes.add(transferir);
        botoes.add(simular);
        botoes.add(exportar);
        rodape.add(botoes, BorderLayout.EAST);
        painelOperacoes.add(rodape, BorderLayout.SOUTH);

        depositar.addActionListener(event -> executarOperacao(
                () -> bancoService.depositar(idContaSelecionada(), lerValor())));
        sacar.addActionListener(event -> executarOperacao(
                () -> bancoService.sacar(idContaSelecionada(), lerValor())));
        transferir.addActionListener(event -> executarOperacao(
                () -> bancoService.transferir(idContaSelecionada(), idContaDestino(), lerValor())));
        simular.addActionListener(event -> executarSimulacao(simular));
        exportar.addActionListener(event -> exportarExtrato());
        return painelOperacoes;
    }

    private JPanel criarPainelExtrato() {
        painelExtrato.setOpaque(true);
        painelExtrato.setBackground(BRANCO);
        painelExtrato.setLayout(new BorderLayout());
        painelExtrato.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINHA),
                BorderFactory.createEmptyBorder(17, 20, 14, 20)));

        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setOpaque(false);
        JLabel titulo = new JLabel("Movimentações recentes");
        titulo.setForeground(TEXTO);
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        JLabel recentes = new JLabel("  EXTRATO DA CONTA");
        recentes.setForeground(TEXTO_SECUNDARIO);
        recentes.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        cabecalho.add(titulo, BorderLayout.WEST);
        cabecalho.add(recentes, BorderLayout.EAST);
        cabecalho.setBorder(BorderFactory.createEmptyBorder(0, 0, 13, 0));
        painelExtrato.add(cabecalho, BorderLayout.NORTH);

        JTable tabela = new JTable(modeloExtrato);
        tabela.setRowHeight(34);
        tabela.setShowGrid(false);
        tabela.setIntercellSpacing(new Dimension(0, 0));
        tabela.setFillsViewportHeight(true);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.getTableHeader().setPreferredSize(new Dimension(0, 30));
        tabela.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        tabela.getTableHeader().setForeground(TEXTO_SECUNDARIO);
        tabela.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        tabela.setForeground(TEXTO);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(138);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(210);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(150);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(120);
        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        rolagem.getViewport().setBackground(BRANCO);
        painelExtrato.add(rolagem, BorderLayout.CENTER);
        painelExtrato.setPreferredSize(new Dimension(600, 280));
        return painelExtrato;
    }

    private static void adicionarCampo(
            JPanel formulario,
            GridBagConstraints constraints,
            int coluna,
            int linha,
            String rotulo,
            Component componente) {
        JPanel grupo = new JPanel();
        grupo.setOpaque(false);
        grupo.setLayout(new javax.swing.BoxLayout(grupo, javax.swing.BoxLayout.Y_AXIS));
        JLabel label = new JLabel(rotulo);
        label.setForeground(TEXTO_SECUNDARIO);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        componente.setPreferredSize(new Dimension(190, 38));
        grupo.add(label);
        grupo.add(javax.swing.Box.createVerticalStrut(7));
        grupo.add(componente);
        constraints.gridx = coluna;
        constraints.gridy = linha;
        formulario.add(grupo, constraints);
    }

    private static JButton criarBotao(String texto, boolean principal) {
        JButton botao = new JButton(texto);
        botao.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        botao.setFocusPainted(false);
        botao.putClientProperty("JButton.buttonType", "roundRect");
        botao.setPreferredSize(new Dimension(112, 37));
        if (principal) {
            botao.setBackground(TURQUESA);
            botao.setForeground(Color.WHITE);
        }
        return botao;
    }

    private static ListCellRenderer<Conta> criarRenderizadorConta() {
        return (lista, conta, indice, selecionado, foco) -> {
            DefaultListCellRenderer renderizador = new DefaultListCellRenderer();
            JLabel rotulo = (JLabel) renderizador.getListCellRendererComponent(
                    lista, conta, indice, selecionado, foco);
            if (conta != null) {
                rotulo.setText("Conta " + conta.getId() + "  ·  " + conta.getTitular().nome());
            }
            return rotulo;
        };
    }

    private void executarOperacao(Runnable operacao) {
        try {
            operacao.run();
            campoValor.setText("");
            atualizarPainel();
            JOptionPane.showMessageDialog(
                    this,
                    "A operação foi registrada com sucesso.",
                    "Operação concluída",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    exception.getMessage(),
                    "Não foi possível concluir a operação",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void executarSimulacao(JButton botaoSimular) {
        int confirmar = JOptionPane.showConfirmDialog(
                this,
                "Executar 48 operacoes bancarias em 4 threads sobre as contas cadastradas?",
                "Simulacao concorrente",
                JOptionPane.OK_CANCEL_OPTION);
        if (confirmar != JOptionPane.OK_OPTION) {
            return;
        }

        definirHabilitado(painelOperacoes, false);
        new SwingWorker<ResultadoSimulacao, Void>() {
            @Override
            protected ResultadoSimulacao doInBackground() {
                return new SimulacaoRunner(bancoService).executar(
                        4, 12, new BigDecimal("1.00"));
            }

            @Override
            protected void done() {
                definirHabilitado(painelOperacoes, true);
                try {
                    ResultadoSimulacao resultado = get();
                    atualizarPainel();
                    JOptionPane.showMessageDialog(
                            BancoFrame.this,
                            "Operacoes realizadas: " + resultado.operacoesRealizadas()
                                    + "\nRecusadas por saldo insuficiente: "
                                    + resultado.operacoesRecusadasPorSaldo()
                                    + "\nSaldos conferidos: "
                                    + (resultado.saldosCorretos() ? "sim" : "nao")
                                    + "\n\nEsperado: " + resultado.saldosEsperados()
                                    + "\nObtido: " + resultado.saldosObtidos(),
                            "Resultado da simulacao",
                            resultado.saldosCorretos()
                                    ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(
                            BancoFrame.this,
                            "A simulacao falhou: " + exception.getMessage(),
                            "Falha na simulacao",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private static void definirHabilitado(Component componente, boolean habilitado) {
        componente.setEnabled(habilitado);
        if (componente instanceof java.awt.Container container) {
            for (Component filho : container.getComponents()) {
                definirHabilitado(filho, habilitado);
            }
        }
    }

    private void exportarExtrato() {
        Conta conta = contaOrigemSelecionada();
        if (conta == null) {
            JOptionPane.showMessageDialog(this, "Selecione uma conta para exportar.");
            return;
        }
        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Salvar extrato da conta " + conta.getId());
        seletor.setSelectedFile(new java.io.File("extrato-conta-" + conta.getId() + ".txt"));
        if (seletor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destino = seletor.getSelectedFile().toPath();
        try {
            new ExtratoExporter().exportar(
                    conta.getId(),
                    conta.getTitular().nome(),
                    bancoService.buscarExtrato(conta.getId()),
                    destino);
            JOptionPane.showMessageDialog(this, "Extrato exportado para:\n" + destino);
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    exception.getMessage(),
                    "Falha ao exportar extrato",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void criarConta() {
        JTextField titular = new JTextField(20);
        JTextField saldoInicial = new JTextField("0,00", 20);
        JPanel formulario = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        formulario.add(new JLabel("Nome do titular:"), constraints);
        constraints.gridx = 1;
        formulario.add(titular, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        formulario.add(new JLabel("Saldo inicial (R$):"), constraints);
        constraints.gridx = 1;
        formulario.add(saldoInicial, constraints);

        int escolha = JOptionPane.showConfirmDialog(
                this,
                formulario,
                "Criar conta bancária",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (escolha != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            String nome = titular.getText().trim();
            if (nome.isEmpty()) {
                throw new IllegalArgumentException("Informe o nome do titular.");
            }
            long novoId = bancoService.listarContas().stream()
                    .mapToLong(Conta::getId)
                    .max()
                    .orElse(0L) + 1L;
            Conta conta = bancoService.criarConta(
                    novoId,
                    new Cliente(novoId, nome),
                    parsearValor(saldoInicial.getText()));
            atualizarContasDisponiveis(conta);
            JOptionPane.showMessageDialog(
                    this,
                    "Conta " + conta.getId() + " criada para " + conta.getTitular().nome() + ".");
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(
                    this, exception.getMessage(), "Não foi possível criar a conta", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void atualizarContasDisponiveis(Conta novaConta) {
        List<Conta> contas = bancoService.listarContas();
        contaOrigem.setModel(new DefaultComboBoxModel<>(contas.toArray(Conta[]::new)));
        contaDestino.setModel(new DefaultComboBoxModel<>(contas.toArray(Conta[]::new)));
        contaOrigem.setSelectedItem(novaConta);
        contaDestino.setSelectedItem(novaConta);
        atualizarPainel();
    }

    private void demonstrarRaceCondition() {
        try {
            var resultado = new DemonstracaoRaceCondition().executar();
            JOptionPane.showMessageDialog(
                    this,
                    "Saldo esperado com 2 depósitos: " + formatarMoeda(resultado.saldoEsperado())
                            + "\nSem sincronização: " + formatarMoeda(resultado.saldoInseguro())
                            + "\nCom Conta sincronizada: " + formatarMoeda(resultado.saldoSincronizado()),
                    "Demonstração de race condition",
                    resultado.saldoEsperado().equals(resultado.saldoSincronizado())
                            ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException exception) {
            JOptionPane.showMessageDialog(
                    this, exception.getMessage(), "Falha na demonstracao", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void atualizarPainel() {
        Conta conta = contaOrigemSelecionada();
        if (conta == null) {
            saldoAtual.setText(formatarMoeda(BigDecimal.ZERO));
            nomeTitular.setText("Nenhuma conta selecionada");
            totalTransacoes.setText("0 movimentações");
            modeloExtrato.setRowCount(0);
            return;
        }

        saldoAtual.setText(formatarMoeda(conta.getSaldo()));
        nomeTitular.setText(conta.getTitular().nome());
        nomeTitularResumo.setText(conta.getTitular().nome());
        List<Transacao> transacoes = bancoService.buscarExtrato(conta.getId());
        totalTransacoes.setText(transacoes.size() + (transacoes.size() == 1
                ? " movimentação" : " movimentações"));
        modeloExtrato.setRowCount(0);
        for (int indice = transacoes.size() - 1; indice >= 0; indice--) {
            Transacao transacao = transacoes.get(indice);
            Long contraparte = transacao.getContaContraparteId();
            modeloExtrato.addRow(new Object[]{
                    transacao.getHorario().format(FORMATADOR_DATA),
                    formatarTipo(transacao.getTipo()),
                    contraparte == null ? "—" : "Conta " + contraparte,
                    formatarMoeda(transacao.getValor())
            });
        }
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
            throw new IllegalArgumentException("Informe o valor da operação.");
        }
        return parsearValor(texto);
    }

    private static BigDecimal parsearValor(String texto) {
        String normalizado = texto.replace(" ", "");
        if (normalizado.contains(",") && normalizado.contains(".")) {
            normalizado = normalizado.replace(".", "").replace(",", ".");
        } else if (normalizado.contains(",")) {
            normalizado = normalizado.replace(",", ".");
        }
        try {
            return new BigDecimal(normalizado);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Informe um valor válido, por exemplo 25,50.");
        }
    }

    private static String formatarTipo(TipoTransacao tipo) {
        return switch (tipo) {
            case DEPOSITO -> "Depósito";
            case SAQUE -> "Saque";
            case TRANSFERENCIA_ENVIADA -> "Transferência enviada";
            case TRANSFERENCIA_RECEBIDA -> "Transferência recebida";
        };
    }

    private static String formatarMoeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(LOCALE_BR).format(valor);
    }

    private static final class Cartao extends JPanel {
        private final Color cor;
        private final int raio;

        private Cartao(Color cor, int raio) {
            this.cor = cor;
            this.raio = raio;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(cor);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), raio, raio);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
