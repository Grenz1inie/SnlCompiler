package com.snl.compiler.ui.theme;

import java.awt.Color;

import javax.swing.UIManager;

public final class UiTheme {
    public static final Color BG_APP = Color.decode("#F4F6FB");
    public static final Color BG_CARD = Color.decode("#FFFFFF");
    public static final Color TEXT_PRIMARY = Color.decode("#1F2937");
    public static final Color TEXT_SECONDARY = Color.decode("#4B5563");
    public static final Color BTN_PRIMARY = Color.decode("#2563EB");
    public static final Color BTN_PRIMARY_HOVER = Color.decode("#1D4ED8");
    public static final Color BTN_PRIMARY_PRESS = Color.decode("#1E40AF");
    public static final Color BTN_PRIMARY_TEXT = Color.WHITE;
    public static final Color BTN_NEUTRAL = Color.decode("#E5E7EB");
    public static final Color BTN_NEUTRAL_HOVER = Color.decode("#D1D5DB");
    public static final Color BTN_NEUTRAL_PRESS = Color.decode("#9CA3AF");
    public static final Color BTN_NEUTRAL_TEXT = Color.decode("#111827");
    public static final Color FOCUS_RING = new Color(147, 197, 253, 180);
    public static final Color BORDER_DEFAULT = Color.decode("#D1D5DB");
    public static final Color BORDER_FOCUS = Color.decode("#3B82F6");

    private UiTheme() {
    }

    public static void applyDefaults() {
        UIManager.put("Panel.background", BG_APP);
        UIManager.put("ToolBar.background", BG_APP);
        UIManager.put("Label.foreground", TEXT_SECONDARY);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("TextArea.background", BG_CARD);
        UIManager.put("TextArea.foreground", TEXT_PRIMARY);
        UIManager.put("TextArea.caretForeground", TEXT_PRIMARY);
        UIManager.put("TextArea.selectionBackground", new Color(191, 219, 254));
        UIManager.put("TextArea.selectionForeground", TEXT_PRIMARY);
        UIManager.put("ComboBox.background", BG_CARD);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
    }
}
