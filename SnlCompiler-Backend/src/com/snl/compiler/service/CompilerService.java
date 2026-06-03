package com.snl.compiler.service;

public class CompilerService {
    private final CompilerPipeline pipeline;

    public CompilerService() {
        this(new CompilerPipeline(new TokenMapper(), new AstRenderer()));
    }

    CompilerService(CompilerPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public synchronized CompileResponse analyzeLexical(CompileRequest request) {
        return pipeline.lexical(request);
    }

    public synchronized CompileResponse analyzeGrammar(CompileRequest request) {
        return pipeline.grammar(request);
    }

    public synchronized CompileResponse analyzeRecursive(CompileRequest request) {
        return pipeline.recursive(request);
    }

    public synchronized CompileResponse analyzeSemantic(CompileRequest request) {
        return pipeline.semantic(request);
    }
}
