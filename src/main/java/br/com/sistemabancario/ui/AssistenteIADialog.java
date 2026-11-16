package br.com.sistemabancario.ui;

import br.com.sistemabancario.service.AssistenteIAService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutionException;

public final class AssistenteIADialog extends JDialog {
    private final AssistenteIAService assistenteIAService;
    private final JTextArea areaHistorico = new JTextArea();
    private final JTextField campoPergunta = new JTextField();
    private final JButton botaoEnviar = new JButton("Enviar");
    private final JLabel estado = new JLabel("Digite sua pergunta e pressione Enter ou Enviar.");

    public AssistenteIADialog(JFrame owner, AssistenteIAService assistenteIAService) {
        super(owner, "Assistente IA", true);
        this.assistenteIAService = assistenteIAService;
        configurarJanela();
        setLocationRelativeTo(owner);
        setSize(new Dimension(720, 520));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                campoPergunta.requestFocusInWindow();
            }
        });
    }

    private void configurarJanela() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(EstiloUI.FUNDO);
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        areaHistorico.setEditable(false);
        areaHistorico.setLineWrap(true);
        areaHistorico.setWrapStyleWord(true);
        areaHistorico.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        areaHistorico.setBackground(java.awt.Color.WHITE);
        areaHistorico.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EstiloUI.BORDA),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        areaHistorico.setText("Assistente: Bem-vindo. Faça uma pergunta sobre seu histórico financeiro.\n");

        JPanel painelPergunta = new JPanel(new BorderLayout(8, 0));
        painelPergunta.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        EstiloUI.aplicarCampoTexto(campoPergunta);
        painelPergunta.add(campoPergunta, BorderLayout.CENTER);
        EstiloUI.aplicarBotao(botaoEnviar, true);
        painelPergunta.add(botaoEnviar, BorderLayout.EAST);

        botaoEnviar.addActionListener(event -> enviarPergunta());
        campoPergunta.addActionListener(event -> enviarPergunta());

        estado.setForeground(EstiloUI.TEXTO_SECUNDARIO);
        JPanel painelInferior = new JPanel(new BorderLayout(8, 6));
        painelInferior.setOpaque(false);
        painelInferior.add(estado, BorderLayout.NORTH);
        painelInferior.add(painelPergunta, BorderLayout.SOUTH);

        add(new JScrollPane(areaHistorico), BorderLayout.CENTER);
        add(painelInferior, BorderLayout.SOUTH);
    }

    private void enviarPergunta() {
        String pergunta = campoPergunta.getText().trim();
        if (pergunta.isBlank() || !botaoEnviar.isEnabled()) {
            return;
        }

        areaHistorico.append("Você: " + pergunta + "\n");
        areaHistorico.append("Assistente: Analisando...\n");
        campoPergunta.setText("");
        campoPergunta.setEnabled(false);
        botaoEnviar.setEnabled(false);
        estado.setText("Consultando o Ollama; isso pode levar alguns segundos...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return assistenteIAService.analisarPergunta(pergunta);
            }

            @Override
            protected void done() {
                botaoEnviar.setEnabled(true);
                campoPergunta.setEnabled(true);
                campoPergunta.requestFocusInWindow();
                try {
                    String resposta = get();
                    areaHistorico.append("Assistente: " + resposta + "\n\n");
                    estado.setText("Resposta recebida.");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    areaHistorico.append("Assistente: A consulta foi interrompida.\n\n");
                    estado.setText("Consulta interrompida.");
                } catch (ExecutionException exception) {
                    Throwable causa = exception.getCause();
                    String detalhe = causa == null ? exception.getMessage() : causa.getMessage();
                    areaHistorico.append("Assistente: " + detalhe + "\n\n");
                    estado.setText("Falha na consulta. Verifique a mensagem acima.");
                }
                areaHistorico.setCaretPosition(areaHistorico.getDocument().getLength());
            }
        };
        worker.execute();
    }
}
