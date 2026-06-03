import com.snl.compiler.core.ast.BaseASTNode;
import com.snl.compiler.core.lexer.Lexer;
import com.snl.compiler.core.parser.RecursiveDescentParser;
import com.snl.compiler.core.semantic.SemanticAnalyzer;
import com.snl.compiler.infra.config.Constants;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DebugSemantic {
    public static void main(String[] args) throws Exception {
        test("bubble", new String(Files.readAllBytes(Paths.get("src/com/snl/compiler/resource/bubble_sort.snl")), "UTF-8"));
        test("4-3", "program p\nvar integer a;\n    integer a;\nbegin\nend.");
    }

    static void test(String name, String src) {
        try {
            Constants.initialize();
            Lexer.doToken(src);
            RecursiveDescentParser rd = new RecursiveDescentParser();
            BaseASTNode root = rd.parse();
            printTree(root, 0);
            SemanticAnalyzer a = new SemanticAnalyzer();
            if (root != null) a.analyze(root);
            System.out.println(name + " SEM: " + a.getErrors());
        } catch (Exception e) {
            System.out.println(name + " EX: " + e);
            e.printStackTrace();
        }
        System.out.println();
    }

    static void printTree(BaseASTNode n, int d) {
        if (n == null) return;
        String indent = "";
        for (int i = 0; i < d; i++) indent += "  ";
        System.out.println(indent + (n.nodeKind == null ? "NULL_KIND" : n.nodeKind)
                + " stm=" + n.stmKind + " dec=" + n.decKind + " names=" + n.name);
        for (int i = 0; i < 3; i++) printTree(n.child[i], d + 1);
        printTree(n.sibling, d);
    }
}
