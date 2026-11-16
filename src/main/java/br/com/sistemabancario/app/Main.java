package br.com.sistemabancario.app;

import br.com.sistemabancario.domain.Cliente;
import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.domain.Usuario;
import br.com.sistemabancario.persistence.ContaRepository;
import br.com.sistemabancario.persistence.UsuarioRepository;
import br.com.sistemabancario.persistence.jdbc.ConnectionFactory;
import br.com.sistemabancario.persistence.jdbc.JdbcContaRepository;
import br.com.sistemabancario.persistence.jdbc.JdbcTransacaoRepository;
import br.com.sistemabancario.persistence.jdbc.JdbcUsuarioRepository;
import br.com.sistemabancario.service.AuthService;
import br.com.sistemabancario.service.BancoService;
import br.com.sistemabancario.service.SessaoUsuario;
import br.com.sistemabancario.ui.BancoFrame;
import br.com.sistemabancario.ui.LoginDialog;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException(
                    "A interface Swing precisa ser executada em um ambiente com tela.");
        }

        BancoFrame.instalarTema();
        SwingUtilities.invokeLater(() -> {
            try {
                ConnectionFactory connectionFactory = new ConnectionFactory();
                connectionFactory.inicializarSchema();

                ContaRepository contaRepository = new JdbcContaRepository(connectionFactory);
                if (contaRepository.listarTodas().isEmpty()) {
                    contaRepository.salvar(new Conta(
                            1, new Cliente(1L, "Ana Silva"), new BigDecimal("1000.00")));
                    contaRepository.salvar(new Conta(
                            2, new Cliente(2L, "Bruno Costa"), new BigDecimal("750.00")));
                }

                UsuarioRepository usuarioRepository = new JdbcUsuarioRepository(connectionFactory);
                AuthService authService = new AuthService(usuarioRepository, contaRepository);
                authService.garantirAdminPadrao();

                LoginDialog dialogo = new LoginDialog(null, authService);
                Usuario usuarioAutenticado = dialogo.getUsuarioAutenticado();
                if (usuarioAutenticado == null) {
                    SessaoUsuario.fechar();
                    return;
                }

                BancoService bancoService = new BancoService(
                        contaRepository, new JdbcTransacaoRepository(connectionFactory));
                BancoFrame janela = new BancoFrame(bancoService, usuarioAutenticado, authService);
                janela.setVisible(true);
            } catch (RuntimeException exception) {
                JOptionPane.showMessageDialog(
                        null,
                        "Nao foi possivel iniciar o sistema: " + exception.getMessage(),
                        "Falha na inicializacao",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
