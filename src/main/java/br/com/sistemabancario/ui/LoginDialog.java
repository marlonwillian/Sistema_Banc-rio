package br.com.sistemabancario.ui;

import br.com.sistemabancario.domain.Usuario;
import br.com.sistemabancario.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class LoginDialog extends JDialog {
    private final AuthService authService;
    private final int maxTentativas;
    private final JTextField campoLogin = new JTextField(20);
    private final JPasswordField campoSenha = new JPasswordField(20);
    private final JLabel mensagemErro = new JLabel(" ");
    private Usuario usuarioAutenticado;
    private int tentativas = 0;

    public LoginDialog(JFrame owner, AuthService authService) {
        super(owner, "Login do Sistema Bancário", true);
        this.authService = authService;
        this.maxTentativas = authService.maxTentativas();
        configurarJanela();
        setLocationRelativeTo(owner);
        setResizable(false);
        setVisible(true);
    }

    public Usuario getUsuarioAutenticado() {
        return usuarioAutenticado;
    }

    private void configurarJanela() {
        JPanel painel = new JPanel(new BorderLayout(18, 18));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        painel.setBackground(EstiloUI.FUNDO);

        JLabel titulo = new JLabel("Sistema Bancário");
        EstiloUI.aplicarLabelTitulo(titulo, 26);
        titulo.setHorizontalAlignment(JLabel.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(8, 0, 8, 0);
        constraints.gridx = 0;
        constraints.gridy = 0;
        form.add(new JLabel("Login:"), constraints);
        EstiloUI.aplicarCampoTexto(campoLogin);
        constraints.gridy = 1;
        form.add(campoLogin, constraints);
        constraints.gridy = 2;
        form.add(new JLabel("Senha:"), constraints);
        EstiloUI.aplicarCampoTexto(campoSenha);
        constraints.gridy = 3;
        form.add(campoSenha, constraints);

        mensagemErro.setForeground(new Color(0xB91C1C));
        mensagemErro.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        mensagemErro.setHorizontalAlignment(JLabel.CENTER);

        JButton entrar = new JButton("Entrar");
        EstiloUI.aplicarBotao(entrar, true);
        entrar.setPreferredSize(new Dimension(180, 40));
        entrar.addActionListener(event -> autenticar());

        JButton cadastrar = new JButton("Criar conta");
        EstiloUI.aplicarBotao(cadastrar, false);
        cadastrar.setPreferredSize(new Dimension(150, 40));
        cadastrar.addActionListener(event -> abrirCadastro());

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.CENTER));
        rodape.setOpaque(false);
        rodape.add(entrar);
        rodape.add(cadastrar);

        JPanel coluna = new JPanel();
        coluna.setLayout(new BoxLayout(coluna, BoxLayout.Y_AXIS));
        coluna.setOpaque(false);
        coluna.add(titulo);
        coluna.add(new JLabel(" "));
        coluna.add(form);
        coluna.add(mensagemErro);
        coluna.add(rodape);

        painel.add(coluna, BorderLayout.CENTER);
        setContentPane(painel);
        setSize(new Dimension(420, 360));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void autenticar() {
        try {
            usuarioAutenticado = authService.autenticar(campoLogin.getText(), campoSenha.getPassword());
            dispose();
        } catch (RuntimeException exception) {
            tentativas++;
            int restantes = maxTentativas - tentativas;
            if (restantes <= 0) {
                mensagemErro.setText("Número máximo de tentativas excedido.");
                dispose();
                return;
            }
            mensagemErro.setText("Credenciais inválidas. Restam " + restantes + " tentativa(s).");
            campoSenha.setText("");
        }
    }

    private void abrirCadastro() {
        CadastroUsuarioDialog cadastro = new CadastroUsuarioDialog(this, authService);
        if (cadastro.getLoginCadastrado() != null) {
            campoLogin.setText(cadastro.getLoginCadastrado());
            campoSenha.requestFocusInWindow();
            mensagemErro.setForeground(new Color(0x15803D));
            mensagemErro.setText("Cadastro realizado. Entre com sua nova senha.");
        }
    }
}
