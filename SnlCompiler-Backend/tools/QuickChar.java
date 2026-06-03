import com.snl.compiler.core.lexer.Lexer;
import com.snl.compiler.core.parser.Parser;
import com.snl.compiler.infra.config.Constants;
import java.nio.file.Files;
import java.nio.file.Paths;

public class QuickChar {
    public static void main(String[] args) throws Exception {
        String src = new String(Files.readAllBytes(Paths.get("src/com/snl/compiler/resource/char_test.snl")), "UTF-8");
        Constants.initialize();
        Lexer.doToken(src);
        System.out.println(Constants.tokenShow2);
        String out = Parser.doGrammar();
        System.out.println("---");
        System.out.println(out);
    }
}
