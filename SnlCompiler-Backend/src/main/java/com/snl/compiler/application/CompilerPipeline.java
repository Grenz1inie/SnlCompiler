package com.snl.compiler.application;

import com.snl.compiler.api.dto.CompileRequest;
import com.snl.compiler.api.dto.CompileResponse;
import com.snl.compiler.application.mapper.SyntaxGraphPreprocessor;
import com.snl.compiler.application.mapper.SyntaxTreeMapper;
import com.snl.compiler.application.mapper.TokenMapper;
import com.snl.compiler.application.render.AstRenderer;
import com.snl.compiler.core.lexer.Lexer;
import com.snl.compiler.core.parser.Parser;
import com.snl.compiler.core.parser.RecursiveDescentParser;
import com.snl.compiler.core.semantic.SemanticAnalyzer;
import com.snl.compiler.infrastructure.config.Constants;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class CompilerPipeline {
    private final TokenMapper tokenMapper;
    private final AstRenderer astRenderer;
    private final SyntaxTreeMapper syntaxTreeMapper;
    private final SyntaxGraphPreprocessor syntaxGraphPreprocessor;
    private boolean initialized;

    CompilerPipeline(TokenMapper tokenMapper, AstRenderer astRenderer, SyntaxTreeMapper syntaxTreeMapper, SyntaxGraphPreprocessor syntaxGraphPreprocessor) {
        this.tokenMapper = tokenMapper;
        this.astRenderer = astRenderer;
        this.syntaxTreeMapper = syntaxTreeMapper;
        this.syntaxGraphPreprocessor = syntaxGraphPreprocessor;
    }

    CompileResponse lexical(CompileRequest request) {
        ensureInitialized();
        CompileResponse response = CompileResponse.of("lexical");
        response.success = Lexer.doToken(normalizeSource(request));
        fillLexicalOutput(response, request);
        if (!response.success) {
            response.errors.add(response.output);
        }
        return response;
    }

    CompileResponse grammar(CompileRequest request) {
        CompileResponse lexical = lexical(request);
        CompileResponse response = CompileResponse.of("grammar");
        response.tokens = lexical.tokens;
        response.externalTokenOutput = lexical.externalTokenOutput;
        response.internalTokenOutput = lexical.internalTokenOutput;
        if (!lexical.success) {
            response.success = false;
            response.output = lexical.output;
            response.errors.addAll(lexical.errors);
            return response;
        }

        response.grammarOutput = Parser.doGrammar();
        CompilationContext context = parseCurrentTokens();
        response.astOutput = astRenderer.render(context.astRoot);
        response.syntaxTree = syntaxTreeMapper.toDtos(context.astRoot);
        response.syntaxGraph = syntaxGraphPreprocessor.toGraph(response.syntaxTree);
        response.errors.addAll(context.parseErrors);

        boolean ll1Success = response.grammarOutput.contains("语法分析成功");
        response.success = ll1Success && response.errors.isEmpty();
        if (!ll1Success) {
            response.errors.add(response.grammarOutput);
        }
        response.output = syntaxReport(response.errors);
        return response;
    }

    CompileResponse recursive(CompileRequest request) {
        CompilationContext context = parse(request);
        CompileResponse response = CompileResponse.of("recursive");
        response.tokens = tokenMapper.toDtos();
        fillTokenRepresentations(response);

        if (!context.lexicalSuccess) {
            response.success = false;
            response.output = chooseTokenOutput(request);
            response.errors.add(response.output);
            return response;
        }

        response.errors.addAll(context.parseErrors);
        response.success = context.astRoot != null && response.errors.isEmpty();
        response.astOutput = astRenderer.render(context.astRoot);
        response.syntaxTree = syntaxTreeMapper.toDtos(context.astRoot);
        response.syntaxGraph = syntaxGraphPreprocessor.toGraph(response.syntaxTree);
        response.output = syntaxReport(response.errors);
        return response;
    }

    CompileResponse semantic(CompileRequest request) {
        CompilationContext context = parse(request);
        CompileResponse response = CompileResponse.of("semantic");
        response.tokens = tokenMapper.toDtos();
        fillTokenRepresentations(response);
        response.astOutput = astRenderer.render(context.astRoot);
        response.syntaxTree = syntaxTreeMapper.toDtos(context.astRoot);
        response.syntaxGraph = syntaxGraphPreprocessor.toGraph(response.syntaxTree);
        response.errors.addAll(context.parseErrors);

        if (!context.lexicalSuccess) {
            response.success = false;
            response.output = chooseTokenOutput(request);
            response.errors.add(response.output);
            return response;
        }

        if (context.astRoot == null || !context.parseErrors.isEmpty()) {
            response.success = false;
            response.output = "--- 递归下降语法错误 ---\n" + join(response.errors)
                    + "\n无法构建语法树，语义分析终止。";
            return response;
        }

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(context.astRoot);
        response.errors.addAll(analyzer.getErrors());
        response.symbolTableOutput = analyzer.getSymbolTable().toString();
        response.success = response.errors.isEmpty();
        response.output = renderSemanticOutput(analyzer.getErrors(), response.symbolTableOutput);
        return response;
    }

    private CompilationContext parse(CompileRequest request) {
        ensureInitialized();
        CompilationContext context = new CompilationContext();
        context.lexicalSuccess = Lexer.doToken(normalizeSource(request));
        if (!context.lexicalSuccess) {
            return context;
        }

        RecursiveDescentParser parser = new RecursiveDescentParser();
        context.astRoot = parser.parse();
        context.parseErrors.addAll(parser.getErrors());
        return context;
    }

    private CompilationContext parseCurrentTokens() {
        CompilationContext context = new CompilationContext();
        context.lexicalSuccess = Constants.token != null && !Constants.token.isEmpty();
        if (!context.lexicalSuccess) {
            return context;
        }

        RecursiveDescentParser parser = new RecursiveDescentParser();
        context.astRoot = parser.parse();
        context.parseErrors.addAll(parser.getErrors());
        return context;
    }

    private void fillLexicalOutput(CompileResponse response, CompileRequest request) {
        fillTokenRepresentations(response);
        response.output = chooseTokenOutput(request);
        response.tokens = tokenMapper.toDtos();
    }

    private void fillTokenRepresentations(CompileResponse response) {
        response.externalTokenOutput = Constants.tokenShow.toString();
        response.internalTokenOutput = Constants.tokenShow2.toString();
    }

    private String chooseTokenOutput(CompileRequest request) {
        if (request != null && "internal".equalsIgnoreCase(request.tokenView)) {
            return Constants.tokenShow2.toString();
        }
        return Constants.tokenShow.toString();
    }

    private String normalizeSource(CompileRequest request) {
        if (request == null || request.source == null) {
            return "";
        }
        return request.source;
    }

    private void ensureInitialized() {
        if (!initialized) {
            Constants.initialize();
            initialized = true;
        }
    }

    private String renderSemanticOutput(List<String> semanticErrors, String symbolTableOutput) {
        StringBuilder output = new StringBuilder();
        output.append("--- 语义检查结果 ---\n");
        if (semanticErrors.isEmpty()) {
            output.append("语义分析成功，未发现错误。\n");
        } else {
            output.append(join(semanticErrors)).append("\n");
        }
        output.append("\n--- 符号表 ---\n");
        output.append(symbolTableOutput);
        return output.toString();
    }

    private String join(List<String> errors) {
        StringBuilder output = new StringBuilder();
        for (String error : errors) {
            output.append(error).append("\n");
        }
        return output.toString();
    }

    private String syntaxReport(List<String> errors) {
        if (errors.isEmpty()) {
            return "语法分析通过。";
        }
        StringBuilder output = new StringBuilder();
        output.append("语法分析发现错误：\n");
        output.append(join(errors));
        return output.toString();
    }
}
