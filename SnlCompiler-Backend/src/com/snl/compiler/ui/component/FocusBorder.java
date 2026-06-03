package com.snl.compiler.ui.component;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.border.AbstractBorder;

import com.snl.compiler.ui.theme.UiTheme;

public class FocusBorder extends AbstractBorder {
    private boolean focused = false;

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(8, 10, 8, 10);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int arc = 12;

        if (focused) {
            g2.setColor(UiTheme.BORDER_FOCUS);
            g2.setColor(new java.awt.Color(59, 130, 246, 45));
            g2.fillRoundRect(x, y, width - 1, height - 1, arc, arc);
            g2.setColor(UiTheme.BORDER_FOCUS);
        } else {
            g2.setColor(UiTheme.BORDER_DEFAULT);
        }

        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
        g2.dispose();
    }
}
