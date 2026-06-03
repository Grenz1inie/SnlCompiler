package com.snl.compiler.ui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

import com.snl.compiler.ui.theme.UiTheme;

public class ModernButtonUI extends BasicButtonUI {
    private final boolean primary;

    public ModernButtonUI(boolean primary) {
        this.primary = primary;
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        JButton b = (JButton) c;
        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 16f)); // 字号保持或微调
        b.setMargin(new Insets(20, 36, 36, 36)); // 减小上边距、大幅增加下边距，为底部立体厚度和阴影留出空间并抬高文字
        b.setForeground(primary ? UiTheme.BTN_PRIMARY_TEXT : UiTheme.BTN_NEUTRAL_TEXT);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        JButton b = (JButton) c;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = b.getWidth();
        int h = b.getHeight();
        int arc = 12; // 随尺寸增大稍微增加圆角，保持比例协调
        int thickness = 6; // 增加立体厚度，配合变大的按钮

        boolean hovered = b.getModel().isRollover();
        boolean pressed = b.getModel().isPressed();
        boolean enabled = b.isEnabled();

        Color base = primary ? UiTheme.BTN_PRIMARY : UiTheme.BTN_NEUTRAL;
        Color hover = primary ? UiTheme.BTN_PRIMARY_HOVER : UiTheme.BTN_NEUTRAL_HOVER;
        Color press = primary ? UiTheme.BTN_PRIMARY_PRESS : UiTheme.BTN_NEUTRAL_PRESS;
        
        if (!enabled) {
            base = new Color(209, 213, 219);
            hover = base;
            press = base;
        }

        Color fill = pressed ? press : (hovered ? hover : base);
        
        // 1. 绘制底层自然投影 (Drop Shadow)
        int shadowAlpha = hovered && !pressed ? 50 : 35;
        g2.setColor(new Color(0, 0, 0, shadowAlpha));
        g2.fillRoundRect(0, 6, w, h - 8, arc, arc);
        g2.setColor(new Color(0, 0, 0, shadowAlpha / 2));
        g2.fillRoundRect(0, 10, w, h - 12, arc, arc);

        // 2. 绘制按钮“基座”（侧面厚度）
        g2.setColor(fill.darker());
        g2.fillRoundRect(0, 0, w, h - 3, arc, arc);

        // 3. 绘制按钮正面 (根据按下状态位移)
        int dy = pressed ? 3 : 0;
        g2.translate(0, dy);
        
        // 使用明显的垂直渐变增强表面弧度感
        GradientPaint surfaceGp = new GradientPaint(0, 0, fill.brighter(), 0, h - thickness, fill);
        g2.setPaint(surfaceGp);
        g2.fillRoundRect(0, 0, w, h - thickness, arc, arc);

        // 4. 正面顶部亮边 (Highlight)
        g2.setColor(new Color(255, 255, 255, 100));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(1, 1, w - 2, h - thickness - 2, arc, arc);

        if (b.isFocusOwner()) {
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(UiTheme.FOCUS_RING);
            g2.drawRoundRect(2, 2, w - 4, h - thickness - 4, arc, arc);
        }

        // 向上偏移文字，使其完美居中在立体的上方部位，而不是连同底部厚度一起居中
        g2.translate(0, -4);
        
        // 绘制文字内容
        super.paint(g2, c);
        
        g2.dispose();
    }
}
