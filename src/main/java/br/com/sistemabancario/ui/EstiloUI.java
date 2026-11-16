package br.com.sistemabancario.ui;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

public final class EstiloUI {
    public static final Color FUNDO = new Color(0xF3F7FB);
    public static final Color FUNDO_PAINEL = Color.WHITE;
    public static final Color AZUL_ESCUDO = new Color(0x123C6F);
    public static final Color AZUL = new Color(0x1D6FD6);
    public static final Color VERDE = new Color(0x1F9D8A);
    public static final Color TEXTO = new Color(0x1E293B);
    public static final Color TEXTO_SECUNDARIO = new Color(0x52607A);
    public static final Color BORDA = new Color(0xD9E2EC);
    public static final Color DESTACADO = new Color(0xEAF5FF);

    private EstiloUI() {
    }

    public static void aplicarTemaPadrao() {
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.selectionBackground", new Color(0xDDEEFF));
        UIManager.put("Table.selectionForeground", TEXTO);
        UIManager.put("Table.gridColor", BORDA);
    }

    public static void aplicarBotao(JButton botao, boolean principal) {
        botao.setFocusPainted(false);
        botao.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        botao.setPreferredSize(new Dimension(120, 38));
        botao.setBackground(principal ? AZUL : FUNDO_PAINEL);
        botao.setForeground(principal ? Color.WHITE : TEXTO);
        botao.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(principal ? AZUL : BORDA, 1, true),
                javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 14)));
    }

    public static void aplicarCampoTexto(Component componente) {
        componente.setBackground(FUNDO_PAINEL);
        componente.setForeground(TEXTO);
        componente.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
    }

    public static void aplicarLabelTitulo(JLabel label, int tamanho) {
        label.setForeground(TEXTO);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, tamanho));
    }

    public static void aplicarPainelPadrao(JPanel painel) {
        painel.setBackground(FUNDO_PAINEL);
        painel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(BORDA, 1, true),
                javax.swing.BorderFactory.createEmptyBorder(14, 16, 14, 16)));
    }

    public static void aplicarTabela(JTable tabela) {
        tabela.setFillsViewportHeight(true);
        tabela.setRowHeight(34);
        tabela.setShowGrid(false);
        tabela.setIntercellSpacing(new java.awt.Dimension(0, 0));
        tabela.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        tabela.setForeground(TEXTO);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.getTableHeader().setBackground(DESTACADO);
        tabela.getTableHeader().setForeground(TEXTO);
        tabela.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
    }

    public static void aplicarCampoSelecao(JComboBox<?> combo) {
        combo.setBackground(FUNDO_PAINEL);
        combo.setForeground(TEXTO);
        combo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        combo.setBorder(javax.swing.BorderFactory.createLineBorder(BORDA));
    }

    public static void aplicarCampoTexto(JTextField campo) {
        campo.setBackground(FUNDO_PAINEL);
        campo.setForeground(TEXTO);
        campo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        campo.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(BORDA, 1, true),
                javax.swing.BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }
}
