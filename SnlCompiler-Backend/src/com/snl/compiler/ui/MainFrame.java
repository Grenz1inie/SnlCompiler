package com.snl.compiler.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

import com.snl.compiler.core.ast.BaseASTNode;
import com.snl.compiler.core.lexer.Lexer;
import com.snl.compiler.core.parser.Parser;
import com.snl.compiler.core.parser.RecursiveDescentParser;
import com.snl.compiler.core.semantic.SemanticAnalyzer;
import com.snl.compiler.infra.config.Constants;
import com.snl.compiler.ui.component.FocusBorder;
import com.snl.compiler.ui.component.ModernButtonUI;
import com.snl.compiler.ui.component.ShadowBorder;
import com.snl.compiler.ui.theme.UiTheme;

public class MainFrame extends JFrame {
    private static final String EXTERNAL_REPR = "外部表示";
    private static final String INTERNAL_REPR = "内部表示";

    private static JTextArea displayJTA = new JTextArea();
    private static JTextArea displayJTA2 = new JTextArea();

    private JButton doTokenJB = new JButton("词法分析");
    private JButton doGrammarJB = new JButton("LL(1)语法分析");
    private JButton doRdGrammarJB = new JButton("递归下降分析");
    private JButton doSemanticJB = new JButton("语义分析");
    private JButton importJB = new JButton("导入文件");
    private JButton helpJB = new JButton("帮助");
    private JComboBox<String> select = new JComboBox<String>();
    private JLabel statusLabel = new JLabel("就绪");

    public MainFrame() {
        setTitle("SNL编译器 - 词法/语法/语义分析");
        initLookAndFeel();
        UiTheme.applyDefaults();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int windowWidth = (int) (0.8 * screenSize.width);
        int windowHeight = (int) (0.8 * screenSize.height);
        setBounds((int) ((screenSize.width - windowWidth) * 0.5),
                (int) ((screenSize.height - windowHeight) * 0.5), windowWidth,
                windowHeight);

        buildUi();
        bindActions();
        bindShortcuts();

        Constants.initialize();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void initLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // 如果 Nimbus 不可用，回退到默认外观
        }
    }

    private void buildUi() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UiTheme.BG_APP);
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JToolBar fileToolBar = new JToolBar();
        fileToolBar.setFloatable(false);
        fileToolBar.setOpaque(false);
        fileToolBar.setBorder(BorderFactory.createEmptyBorder(0, 6, 8, 6));
        
        importJB.setUI(new ModernButtonUI(false));
        fileToolBar.add(importJB);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setOpaque(false);
        toolBar.setBorder(BorderFactory.createEmptyBorder(6, 6, 10, 6));

        select.addItem(EXTERNAL_REPR);
        select.addItem(INTERNAL_REPR);
        select.setMaximumSize(new Dimension(150, 40)); // 调大尺寸
        select.setFont(resolveTextFont().deriveFont(Font.PLAIN, 14f));
        select.setBackground(UiTheme.BG_CARD);
        select.setForeground(UiTheme.TEXT_PRIMARY);

        doTokenJB.setUI(new ModernButtonUI(true));
        doGrammarJB.setUI(new ModernButtonUI(true));
        doRdGrammarJB.setUI(new ModernButtonUI(true));
        doSemanticJB.setUI(new ModernButtonUI(true));
        helpJB.setUI(new ModernButtonUI(false));

        toolBar.add(new JLabel("Token表示: "));
        toolBar.add(select);
        toolBar.addSeparator(new Dimension(20, 0)); // 增加间距
        toolBar.add(doTokenJB);
        toolBar.addSeparator(new Dimension(12, 0)); // 增加按钮间的间距
        toolBar.add(doGrammarJB);
        toolBar.addSeparator(new Dimension(12, 0)); // 增加按钮间的间距
        toolBar.add(doRdGrammarJB);
        toolBar.addSeparator(new Dimension(12, 0)); // 增加按钮间的间距
        toolBar.add(doSemanticJB);
        toolBar.addSeparator(new Dimension(20, 0)); // 增加间距
        toolBar.add(helpJB);

        topPanel.add(fileToolBar);
        topPanel.add(toolBar);
        add(topPanel, BorderLayout.NORTH);

        Font textFont = resolveTextFont();
        displayJTA.setFont(textFont.deriveFont(Font.PLAIN, 15f));
        displayJTA2.setFont(textFont.deriveFont(Font.PLAIN, 15f));
        displayJTA.setBackground(UiTheme.BG_CARD);
        displayJTA2.setBackground(UiTheme.BG_CARD);
        displayJTA.setForeground(UiTheme.TEXT_PRIMARY);
        displayJTA2.setForeground(UiTheme.TEXT_PRIMARY);

        displayJTA.setLineWrap(true);
        displayJTA.setWrapStyleWord(true);
        displayJTA2.setLineWrap(true);
        displayJTA2.setWrapStyleWord(true);
        displayJTA2.setEditable(false);

        FocusBorder inputBorder = new FocusBorder();
        FocusBorder outputBorder = new FocusBorder();
        displayJTA.setBorder(inputBorder);
        displayJTA2.setBorder(outputBorder);
        displayJTA.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                inputBorder.setFocused(true);
                displayJTA.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                inputBorder.setFocused(false);
                displayJTA.repaint();
            }
        });
        displayJTA2.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                outputBorder.setFocused(true);
                displayJTA2.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                outputBorder.setFocused(false);
                displayJTA2.repaint();
            }
        });

        JScrollPane inputScroll = new JScrollPane(displayJTA);
        JScrollPane outputScroll = new JScrollPane(displayJTA2);
        inputScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("源码输入"),
                new EmptyBorder(6, 6, 6, 6)
        ));
        outputScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("分析输出"),
                new EmptyBorder(6, 6, 6, 6)
        ));
        inputScroll.getViewport().setBackground(UiTheme.BG_CARD);
        outputScroll.getViewport().setBackground(UiTheme.BG_CARD);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputScroll, outputScroll);
        splitPane.setResizeWeight(0.5);
        splitPane.setOneTouchExpandable(true);
        add(splitPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false);
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER_DEFAULT),
                new EmptyBorder(8, 10, 6, 10)
        ));
        statusLabel.setForeground(UiTheme.TEXT_SECONDARY);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 13f));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private Font resolveTextFont() {
        String[] candidates = {"Microsoft YaHei UI", "PingFang SC", "Noto Sans CJK SC", "Dialog"};
        for (String name : candidates) {
            Font f = new Font(name, Font.PLAIN, 15);
            if (f != null && f.canDisplay('语')) {
                return f;
            }
        }
        return new Font("Dialog", Font.PLAIN, 15);
    }

    private void bindActions() {
        importJB.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(new File("src/com/snl/compiler/resource"));
            int option = fileChooser.showOpenDialog(MainFrame.this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    displayJTA.setText(content);
                    statusLabel.setText("文件导入成功: " + file.getName());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, "读取文件失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("文件导入失败");
                }
            }
        });

        doTokenJB.addActionListener(e -> runLexicalAnalysis());

        doGrammarJB.setEnabled(false);
        doGrammarJB.addActionListener(e -> runGrammarAnalysis());

        doRdGrammarJB.setEnabled(false);
        doRdGrammarJB.addActionListener(e -> runRdGrammarAnalysis());

        doSemanticJB.setEnabled(false);
        doSemanticJB.addActionListener(e -> runSemanticAnalysis());

        helpJB.addActionListener(e -> {
            String s = "1. Token 外部表示：1分隔符；2保留字；3标识符；4整数；5字符常量。"
                    + "\n   内部表示格式：(行号,类型,下标)。注释 { ... } 会被跳过，不进入 Token 序列。"
                    + "\n2. LL(1)语法分析输出推导规则编号；递归下降分析输出语法错误信息。"
                    + "\n3. 词法失败时语法/语义按钮不可用；LL(1)成功后方可进行语义分析。"
                    + "\n4. 语法分析时将标识符统一为 ID，整数统一为 INTC。";
            JOptionPane pane = new JOptionPane(s, JOptionPane.INFORMATION_MESSAGE);
            JDialog dialog = pane.createDialog(MainFrame.this, "帮助");
            dialog.getRootPane().setBorder(BorderFactory.createCompoundBorder(
                    new ShadowBorder(14, 6),
                    new EmptyBorder(8, 10, 8, 10)
            ));
            dialog.setVisible(true);
        });
    }

    private void bindShortcuts() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "runToken");
        actionMap.put("runToken", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runLexicalAnalysis();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "runGrammar");
        actionMap.put("runGrammar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (doGrammarJB.isEnabled()) {
                    runGrammarAnalysis();
                }
            }
        });
    }

    private void runLexicalAnalysis() {
        boolean success = Lexer.doToken(displayJTA.getText());
        doGrammarJB.setEnabled(success);
        doRdGrammarJB.setEnabled(success);
        doSemanticJB.setEnabled(false);

        if (EXTERNAL_REPR.equals(select.getSelectedItem().toString())) {
            displayJTA2.setText(Constants.tokenShow.toString());
        } else {
            displayJTA2.setText(Constants.tokenShow2.toString());
        }

        if (success) {
            statusLabel.setText("词法分析成功，可进行语法分析。");
        } else {
            statusLabel.setText("词法分析失败，请检查输入后重试。");
        }
    }

    private void runGrammarAnalysis() {
        String result = Parser.doGrammar();
        displayJTA2.setText(result);
        if (result.contains("语法分析成功")) {
            doSemanticJB.setEnabled(true);
            statusLabel.setText("LL(1)语法分析完成，可进行语义分析。");
        } else {
            doSemanticJB.setEnabled(false);
            statusLabel.setText("LL(1)语法分析失败。");
        }
    }

    private void runRdGrammarAnalysis() {
        RecursiveDescentParser rdParser = new RecursiveDescentParser();
        rdParser.parse();
        StringBuilder output = new StringBuilder();
        output.append("--- 递归下降语法分析 ---\n");
        if (rdParser.getErrors().isEmpty()) {
            output.append("递归下降语法分析成功，未发现语法错误。\n");
            doSemanticJB.setEnabled(true);
            statusLabel.setText("递归下降语法分析成功，可进行语义分析。");
        } else {
            for (String err : rdParser.getErrors()) {
                output.append(err).append("\n");
            }
            output.append("递归下降语法分析失败。\n");
            doSemanticJB.setEnabled(false);
            statusLabel.setText("递归下降语法分析失败。");
        }
        displayJTA2.setText(output.toString());
    }

    private void runSemanticAnalysis() {
        RecursiveDescentParser rdParser = new RecursiveDescentParser();
        BaseASTNode root = rdParser.parse();
        
        StringBuilder output = new StringBuilder();
        if (!rdParser.getErrors().isEmpty()) {
            output.append("--- 递归下降语法错误 ---\n");
            for (String err : rdParser.getErrors()) {
                output.append(err).append("\n");
            }
        }

        if (root != null && rdParser.getErrors().isEmpty()) {
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(root);
            
            output.append("\n--- 语义检查结果 ---\n");
            if (analyzer.getErrors().isEmpty()) {
                output.append("语义分析成功，未发现错误。\n");
            } else {
                for (String err : analyzer.getErrors()) {
                    output.append(err).append("\n");
                }
            }
            
            output.append("\n--- 符号表 ---\n");
            output.append(analyzer.getSymbolTable().toString());
        } else {
            output.append("\n无法构建语法树，语义分析终止。");
        }
        
        displayJTA2.setText(output.toString());
        statusLabel.setText("语义分析完成。");
    }

    public static void main(String[] args) {
        new MainFrame();
    }
}