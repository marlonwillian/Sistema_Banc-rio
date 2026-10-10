package br.com.sistemabancario.app;

import br.com.sistemabancario.domain.Cliente;
import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.persistence.memory.InMemoryTransacaoRepository;
import br.com.sistemabancario.service.BancoService;
import br.com.sistemabancario.ui.BancoFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;
import java.util.List;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException(
                    "A interface Swing precisa ser executada em um ambiente com tela.");
        }

        SwingUtilities.invokeLater(() -> {
            List<Conta> contas = List.of(
                    new Conta(1, new Cliente(1L, "Ana Silva"), new BigDecimal("1000.00")),
                    new Conta(2, new Cliente(2L, "Bruno Costa"), new BigDecimal("750.00")));
            BancoService bancoService = new BancoService(contas, new InMemoryTransacaoRepository());
            BancoFrame janela = new BancoFrame(bancoService);
            janela.setVisible(true);
        });
    }
}
