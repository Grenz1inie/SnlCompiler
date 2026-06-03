import com.snl.compiler.core.lexer.Lexer;
import com.snl.compiler.core.parser.Parser;
import com.snl.compiler.infra.config.Constants;

public class QuickVerify {
    public static void main(String[] args) {
        Constants.initialize();
        String src = "program p\n" +
                "type t = integer ;\n" +
                "var t v1;\n" +
                "    char v2;\n" +
                "begin\n" +
                "read(v1);\n" +
                "    v1:=v1+10;\n" +
                "    write(v1)\n" +
                "end.";
        System.out.println("=== 词法分析 ===");
        boolean lexOk = Lexer.doToken(src);
        System.out.println(Constants.tokenShow.toString());
        System.out.println("词法结果: " + (lexOk ? "成功" : "失败"));
        if (lexOk) {
            System.out.println("\n=== 语法分析 ===");
            System.out.println(Parser.doGrammar());
        }
    }
}
