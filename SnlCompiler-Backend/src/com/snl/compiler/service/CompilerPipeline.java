package com.snl.compiler.service;

import com.snl.compiler.core.lexer.Lexer;
import com.snl.compiler.core.parser.Parser;
import com.snl.compiler.core.parser.RecursiveDescentParser;
import com.snl.compiler.core.semantic.SemanticAnalyzer;
import com.snl.compiler.infra.config.Constants;

import java.util.List;

class CompilerPipeline {
    private final TokenMapper tokenMapper;
    private final AstRenderer astRenderer;
    private boolean initialized;

    CompilerPipeline(TokenMapper tokenMapper, AstRenderer astRenderer) {
        this.tokenMapper = tokenMapper;
        this.astRenderer = astRenderer;
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
        if (!lexical.success) {
            response.success = false;
            response.output = lexical.output;
            response.errors.addAll(lexical.errors);
            return response;
        }

        response.grammarOutput = Parser.doGrammar();
        response.output = response.grammarOutput;
        response.success = response.output.contains("语法分析成功");
        if (!response.success) {
            response.errors.add(response.output);
        }
        return response;
    }

    CompileResponse recursive(CompileRequest request) {
        CompilationContext context = parse(request);
        CompileResponse response = CompileResponse.of("recursive");
        response.tokens = tokenMapper.toDtos();

        if (!context.lexicalSuccess) {
            response.success = false;
            response.output = chooseTokenOutput(request);
            response.errors.add(response.output);
            return response;
        }

        response.errors.addAll(context.parseErrors);
        response.success = context.astRoot != null && response.errors.isEmpty();
        response.astOutput = astRenderer.render(context.astRoot);
        response.output = response.success ? response.astOutput : join(response.errors);
        return response;
    }

    CompileResponse semantic(CompileRequest request) {
        CompilationContext context = parse(request);
        CompileResponse response = CompileResponse.of("semantic");
        response.tokens = tokenMapper.toDtos();
        response.astOutput = astRenderer.render(context.astRoot);
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

    private void fillLexicalOutput(CompileResponse response, CompileRequest request) {
        response.externalTokenOutput = Constants.tokenShow.toString();
        response.internalTokenOutput = Constants.tokenShow2.toString();
        response.output = chooseTokenOutput(request);
        response.tokens = tokenMapper.toDtos();
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
}
