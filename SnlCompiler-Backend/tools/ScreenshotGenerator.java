import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.snl.compiler.core.ast.BaseASTNode;
import com.snl.compiler.core.lexer.Lexer;
import com.snl.compiler.core.parser.Parser;
import com.snl.compiler.core.parser.RecursiveDescentParser;
import com.snl.compiler.core.semantic.SemanticAnalyzer;
import com.snl.compiler.infra.config.Constants;

public class ScreenshotGenerator {
    private static final int W = 1280;
    private static final int H = 800;
    private static final Color BG = new Color(0xF4F6FB);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = new Color(0xD1D5DB);
    private static final Color TEXT = new Color(0x1F2937);
    private static final Color MUTED = new Color(0x4B5563);
    private static final Color BTN = new Color(0x2563EB);
    private static final Color BTN_OFF = new Color(0xE5E7EB);
    private static final String OUT_DIR = "screenshots";

    private static Font fontUi;
    private static Font fontCode;
    private static Font fontBtn;

    public static void main(String[] args) throws Exception {
        initFonts();
        new File(OUT_DIR).mkdirs();
        Constants.initialize();

        capture("00_主界面说明.png", sampleSource(), "（启动后可见此界面布局）\n\n"
                + "顶部按钮：词法分析 | LL(1)语法分析 | 递归下降分析 | 语义分析\n"
                + "左侧：源码输入    右侧：分析输出\n\n状态栏：就绪",
                "就绪", new boolean[] { true, false, false, false }, "外部表示");

        runLex("01_词法分析_sample_外部表示.png", read("sample.snl"), false);
        runLex("02_词法分析_sample_内部表示.png", read("sample.snl"), true);
        runLex("03_词法分析_注释程序.png", read("comment_test.snl"), false);
        runLex("04_词法分析_字符常量.png", read("char_test.snl"), false);
        runLex("05_词法错误_非法字符.png", "program p#\nbegin\nend.", false);
        runLex("06_词法错误_缺少句点.png", "program p\nbegin\nend", false);

        runLl1("07_LL1语法分析_成功.png", read("sample.snl"));
        runLl1("08_LL1语法分析_冒泡排序.png", read("bubble_sort.snl"));
        runLl1("09_LL1语法错误.png", "program p\nbegin\nread(v1).");
        runLl1("15_LL1语法分析_字符常量.png", read("char_test.snl"));

        runRd("10_递归下降_成功.png", read("sample.snl"));
        runRd("11_递归下降_record类型.png", read("record_test.snl"));
        runRd("16_递归下降_字符常量.png", read("char_test.snl"));

        runSemantic("12_语义分析_成功.png", read("sample.snl"));
        runSemantic("13_语义错误_未声明.png", "program p\nbegin\n    x := 1\nend.");
        runSemantic("14_语义错误_重复定义.png",
                "program p\nvar integer a;\n    integer a;\nbegin\nend.");

        System.out.println("Screenshots saved to " + new File(OUT_DIR).getAbsolutePath());
    }

    private static void initFonts() {
        String[] names = { "Microsoft YaHei UI", "SimSun", "Dialog" };
        Font base = new Font("Dialog", Font.PLAIN, 14);
        for (String n : names) {
            Font f = new Font(n, Font.PLAIN, 14);
            if (f.canDisplay('语')) {
                base = f;
                break;
            }
        }
        fontUi = base.deriveFont(Font.PLAIN, 14f);
        fontCode = base.deriveFont(Font.PLAIN, 13f);
        fontBtn = base.deriveFont(Font.BOLD, 13f);
    }

    private static String read(String name) throws Exception {
        return new String(Files.readAllBytes(
                Paths.get("src/com/snl/compiler/resource/" + name)), StandardCharsets.UTF_8);
    }

    private static String sampleSource() throws Exception {
        return read("sample.snl");
    }

    private static void runLex(String file, String src, boolean internal) {
        Constants.initialize();
        boolean ok = Lexer.doToken(src);
        String out = internal ? Constants.tokenShow2.toString() : Constants.tokenShow.toString();
        capture(file, src, out,
                ok ? "词法分析成功，可进行语法分析。" : "词法分析失败，请检查输入后重试。",
                new boolean[] { true, ok, ok, false },
                internal ? "内部表示" : "外部表示");
    }

    private static void runLl1(String file, String src) {
        Constants.initialize();
        Lexer.doToken(src);
        String out = truncateMiddle(Parser.doGrammar(), 120, 40);
        boolean ok = out.contains("语法分析成功");
        capture(file, src, out,
                ok ? "LL(1)语法分析完成，可进行语义分析。" : "LL(1)语法分析失败。",
                new boolean[] { true, true, true, ok },
                "外部表示");
    }

    private static void runRd(String file, String src) {
        Constants.initialize();
        Lexer.doToken(src);
        RecursiveDescentParser rd = new RecursiveDescentParser();
        rd.parse();
        StringBuilder sb = new StringBuilder("--- 递归下降语法分析 ---\n");
        if (rd.getErrors().isEmpty()) {
            sb.append("递归下降语法分析成功，未发现语法错误。");
        } else {
            for (String e : rd.getErrors()) {
                sb.append(e).append('\n');
            }
            sb.append("递归下降语法分析失败。");
        }
        boolean ok = rd.getErrors().isEmpty();
        capture(file, src, sb.toString(),
                ok ? "递归下降语法分析成功，可进行语义分析。" : "递归下降语法分析失败。",
                new boolean[] { true, true, true, ok },
                "外部表示");
    }

    private static void runSemantic(String file, String src) {
        Constants.initialize();
        Lexer.doToken(src);
        Parser.doGrammar();
        RecursiveDescentParser rd = new RecursiveDescentParser();
        BaseASTNode root = rd.parse();
        StringBuilder sb = new StringBuilder();
        if (!rd.getErrors().isEmpty()) {
            sb.append("--- 递归下降语法错误 ---\n");
            for (String e : rd.getErrors()) {
                sb.append(e).append('\n');
            }
        }
        if (root != null && rd.getErrors().isEmpty()) {
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(root);
            sb.append("\n--- 语义检查结果 ---\n");
            if (analyzer.getErrors().isEmpty()) {
                sb.append("语义分析成功，未发现错误。\n");
            } else {
                for (String e : analyzer.getErrors()) {
                    sb.append(e).append('\n');
                }
            }
            sb.append("\n--- 符号表 ---\n");
            sb.append(analyzer.getSymbolTable().toString());
        } else {
            sb.append("\n无法构建语法树，语义分析终止。");
        }
        capture(file, src, sb.toString(), "语义分析完成。",
                new boolean[] { true, true, true, true }, "外部表示");
    }

    private static String truncateMiddle(String text, int headLines, int tailLines) {
        String[] lines = text.split("\n", -1);
        if (lines.length <= headLines + tailLines + 2) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headLines; i++) {
            sb.append(lines[i]).append('\n');
        }
        sb.append("... （中间推导过程省略，共 ").append(lines.length).append(" 行） ...\n");
        for (int i = lines.length - tailLines; i < lines.length; i++) {
            sb.append(lines[i]);
            if (i < lines.length - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static void capture(String filename, String source, String output, String status,
            boolean[] btnEnabled, String tokenMode) {
        try {
            BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(BG);
            g.fillRect(0, 0, W, H);

            g.setColor(TEXT);
            g.setFont(fontUi.deriveFont(Font.BOLD, 18f));
            g.drawString("SNL编译器 - 词法/语法/语义分析", 24, 36);

            drawToolbar(g, btnEnabled, tokenMode);

            int top = 110;
            int panelH = H - top - 50;
            int gap = 16;
            int panelW = (W - 48 - gap) / 2;
            drawPanel(g, 24, top, panelW, panelH, "源码输入", source, true);
            drawPanel(g, 24 + panelW + gap, top, panelW, panelH, "分析输出", output, false);

            g.setColor(BORDER);
            g.drawLine(24, H - 42, W - 24, H - 42);
            g.setColor(MUTED);
            g.setFont(fontUi);
            g.drawString("状态：" + status, 32, H - 16);

            g.dispose();
            ImageIO.write(img, "png", new File(OUT_DIR, filename));
            System.out.println("OK " + filename);
        } catch (Exception e) {
            System.err.println("FAIL " + filename + ": " + e.getMessage());
        }
    }

    private static void drawToolbar(Graphics2D g, boolean[] enabled, String tokenMode) {
        int x = 24;
        int y = 56;
        g.setFont(fontUi);
        g.setColor(MUTED);
        g.drawString("Token表示: " + tokenMode, x, y + 22);
        x += 160;

        String[] labels = { "词法分析", "LL(1)语法分析", "递归下降分析", "语义分析" };
        for (int i = 0; i < labels.length; i++) {
            int bw = 130;
            if (i == 1) {
                bw = 150;
            }
            g.setColor(enabled[i] ? BTN : BTN_OFF);
            g.fillRoundRect(x, y, bw, 34, 10, 10);
            g.setColor(enabled[i] ? Color.WHITE : TEXT);
            g.setFont(fontBtn);
            g.drawString(labels[i], x + 12, y + 22);
            x += bw + 10;
        }
    }

    private static void drawPanel(Graphics2D g, int x, int y, int w, int h, String title,
            String content, boolean editable) {
        g.setColor(BORDER);
        g.drawRoundRect(x, y, w, h, 12, 12);
        g.setColor(CARD);
        g.fillRoundRect(x + 1, y + 1, w - 2, h - 2, 12, 12);

        g.setColor(MUTED);
        g.setFont(fontUi.deriveFont(Font.BOLD, 13f));
        g.drawString(title, x + 14, y + 22);

        int tx = x + 12;
        int ty = y + 34;
        int tw = w - 24;
        int th = h - 46;
        g.setClip(tx, ty, tw, th);
        g.setColor(TEXT);
        g.setFont(fontCode);
        List<String> lines = wrapLines(content, g.getFontMetrics(fontCode), tw);
        int lineH = g.getFontMetrics(fontCode).getHeight();
        int maxLines = th / lineH;
        int start = 0;
        if (lines.size() > maxLines) {
            start = lines.size() - maxLines;
        }
        for (int i = start; i < lines.size(); i++) {
            g.drawString(lines.get(i), tx + 4, ty + (i - start + 1) * lineH - 4);
        }
        g.setClip(null);

        if (editable) {
            g.setColor(new Color(0x3B82F6));
            g.drawRect(tx, ty, tw - 1, th - 1);
        }
    }

    private static List<String> wrapLines(String text, FontMetrics fm, int maxWidth) {
        List<String> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        String[] raw = text.replace("\r", "").split("\n", -1);
        for (String line : raw) {
            if (line.isEmpty()) {
                result.add("");
                continue;
            }
            while (line.length() > 0) {
                int fit = line.length();
                while (fit > 0 && fm.stringWidth(line.substring(0, fit)) > maxWidth) {
                    fit--;
                }
                if (fit == 0) {
                    fit = 1;
                }
                result.add(line.substring(0, fit));
                line = line.substring(fit);
            }
        }
        return result;
    }
}
