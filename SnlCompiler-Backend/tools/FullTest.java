import com.snl.compiler.core.ast.BaseASTNode;
import com.snl.compiler.core.lexer.Lexer;
import com.snl.compiler.core.parser.Parser;
import com.snl.compiler.core.parser.RecursiveDescentParser;
import com.snl.compiler.core.semantic.SemanticAnalyzer;
import com.snl.compiler.infra.config.Constants;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 覆盖《实验测试文档.md》全部用例的自动化测试。
 */
public class FullTest {
    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
    }

    private static void testProgram(String name, String source,
            boolean expectLex, boolean expectLl1, boolean expectRd, boolean expectSemantic) {
        Constants.initialize();
        boolean lex = Lexer.doToken(source);
        check(name + " / 词法", lex == expectLex);
        if (!lex) {
            return;
        }

        String ll1 = Parser.doGrammar();
        check(name + " / LL(1)", ll1.contains("语法分析成功") == expectLl1);

        RecursiveDescentParser rd = new RecursiveDescentParser();
        rd.parse();
        check(name + " / 递归下降", rd.getErrors().isEmpty() == expectRd);

        Constants.initialize();
        Lexer.doToken(source);
        RecursiveDescentParser rd2 = new RecursiveDescentParser();
        BaseASTNode root = rd2.parse();
        if (expectSemantic) {
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            if (root != null) {
                analyzer.analyze(root);
            }
            check(name + " / 语义", root != null && analyzer.getErrors().isEmpty());
        } else if (root != null && rd2.getErrors().isEmpty()) {
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(root);
            check(name + " / 语义(应失败)", !analyzer.getErrors().isEmpty());
        }
    }

    private static void testLexOnly(String name, String source, boolean expectOk) {
        Constants.initialize();
        check(name, Lexer.doToken(source) == expectOk);
    }

    private static void testLl1Only(String name, String source, boolean expectOk) {
        Constants.initialize();
        Lexer.doToken(source);
        String out = Parser.doGrammar();
        check(name, out.contains("语法分析成功") == expectOk);
    }

    private static void testSemanticErrors(String name, String source, String mustContain) {
        Constants.initialize();
        Lexer.doToken(source);
        RecursiveDescentParser rd = new RecursiveDescentParser();
        BaseASTNode root = rd.parse();
        boolean rdOk = rd.getErrors().isEmpty();
        check(name + " / 语法(前置)", rdOk);
        if (!rdOk || root == null) {
            return;
        }
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(root);
        List<String> errs = analyzer.getErrors();
        boolean hasErr = !errs.isEmpty();
        check(name + " / 语义(应报错)", hasErr);
        if (hasErr && mustContain != null) {
            String joined = String.join(" ", errs);
            check(name + " / 错误含「" + mustContain + "」", joined.contains(mustContain));
        }
    }

    public static void main(String[] args) throws Exception {
        String base = "src/com/snl/compiler/resource/";

        System.out.println("=== 1. 资源文件正向测试 ===");
        testProgram("1-1 sample", read(base + "sample.snl"), true, true, true, true);
        testProgram("1-3 comment_test", read(base + "comment_test.snl"), true, true, true, true);
        testProgram("1-4 char_test", read(base + "char_test.snl"), true, true, true, true);
        testProgram("2-2 bubble_sort", read(base + "bubble_sort.snl"), true, true, true, true);
        testProgram("3-2 record_test", read(base + "record_test.snl"), true, true, true, true);

        System.out.println("=== 2. 词法错误测试 ===");
        testLexOnly("1-5 非法字符", "program p#\nbegin\nend.", false);
        testLexOnly("1-6 缺少句点", "program p\nbegin\nend", false);

        System.out.println("=== 3. 语法错误测试 ===");
        testLl1Only("2-3 LL1语法错误", "program p\nbegin\nread(v1).", false);

        Constants.initialize();
        Lexer.doToken("program p\nbegin\nread(v1).");
        RecursiveDescentParser rd = new RecursiveDescentParser();
        rd.parse();
        check("2-3 递归下降语法错误", !rd.getErrors().isEmpty());

        System.out.println("=== 4. 语义错误测试 ===");
        testSemanticErrors("4-2 未声明变量", "program p\nbegin\n    x := 1\nend.", "未声明");
        testSemanticErrors("4-3 重复定义", "program p\nvar integer a;\n    integer a;\nbegin\nend.", "重复");

        System.out.println("=== 汇总 ===");
        System.out.println("TOTAL PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }
}
