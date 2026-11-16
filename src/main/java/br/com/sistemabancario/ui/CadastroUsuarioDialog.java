package br.com.sistemabancario.ui;

import br.com.sistemabancario.domain.Usuario;
import br.com.sistemabancario.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

public final class CadastroUsuarioDialog extends JDialog {
    private final AuthService authService;
    private final JTextField campoNome = new JTextField(22);
    private final JTextField campoLogin = new JTextField(22);
    private final JPasswordField campoSenha = new JPasswordField(22);
    private final JPasswordField campoConfirmacao = new JPasswordField(22);
    private final JLabel mensagem = new JLabel(" ");
    private String loginCadastrado;

    public CadastroUsuarioDialog(JDialog owner, AuthService authService) {
        super(owner, "Cadastro de usuário", true);
        this.authService = authService;
        configurarJanela();
        setLocationRelativeTo(owner);
        setVisible(true);
    }

    public String getLoginCadastrado() {
        return loginCadastrado;
    }

    private void configurarJanela() {
        JPanel painel = new JPanel(new BorderLayout(12, 12));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        painel.setBackground(EstiloUI.FUNDO);

        JLabel titulo = new JLabel("Criar conta de usuário");
        EstiloUI.aplicarLabelTitulo(titulo, 22);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);
        adicionarCampo(formulario, "Nome do titular:", campoNome, 0);
        adicionarCampo(formulario, "Login:", campoLogin, 1);
        adicionarCampo(formulario, "Senha (mínimo 6 caracteres):", campoSenha, 2);
        adicionarCampo(formulario, "Confirme a senha:", campoConfirmacao, 3);

        mensagem.setForeground(EstiloUI.TEXTO_SECUNDARIO);

        JButton confirmar = new JButton("Cadastrar");
        EstiloUI.aplicarBotao(confirmar, true);
        confirmar.setPreferredSize(new Dimension(150, 40));
        confirmar.addActionListener(event -> cadastrar());

        JButton cancelar = new JButton("Cancelar");
        EstiloUI.aplicarBotao(cancelar, false);
        cancelar.setPreferredSize(new Dimension(120, 40));
        cancelar.addActionListener(event -> dispose());

        JPanel botoes = new JPanel();
        botoes.setOpaque(false);
        botoes.add(cancelar);
        botoes.add(confirmar);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.add(mensagem, BorderLayout.NORTH);
        rodape.add(botoes, BorderLayout.EAST);

        painel.add(titulo, BorderLayout.NORTH);
        painel.add(formulario, BorderLayout.CENTER);
        painel.add(rodape, BorderLayout.SOUTH);
        setContentPane(painel);
        setSize(new Dimension(540, 390));
        setMinimumSize(new Dimension(520, 370));
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void cadastrar() {
        char[] senha = campoSenha.getPassword();
        char[] confirmacao = campoConfirmacao.getPassword();
        try {
            if (!Arrays.equals(senha, confirmacao)) {
                mensagem.setForeground(new java.awt.Color(0xB91C1C));
                mensagem.setText("As senhas não conferem.");
                return;
            }

            Usuario usuario = authService.cadastrarUsuario(
                    campoLogin.getText(), senha, campoNome.getText());
            loginCadastrado = usuario.login();
            mensagem.setForeground(new java.awt.Color(0x15803D));
            mensagem.setText("Conta nº " + usuario.contaId() + " criada com saldo inicial R$ 0,00.");
            campoSenha.setText("");
            campoConfirmacao.setText("");
            dispose();
        } catch (RuntimeException exception) {
            mensagem.setForeground(new java.awt.Color(0xB91C1C));
            mensagem.setText(exception.getMessage());
        } finally {
            Arrays.fill(senha, '\0');
            Arrays.fill(confirmacao, '\0');
        }
    }

    private static void adicionarCampo(JPanel formulario, String rotulo, JTextField campo, int linha) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = linha;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(6, 0, 6, 10);
        formulario.add(new JLabel(rotulo), constraints);

        EstiloUI.aplicarCampoTexto(campo);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(4, 0, 4, 0);
        formulario.add(campo, constraints);
    }
}
