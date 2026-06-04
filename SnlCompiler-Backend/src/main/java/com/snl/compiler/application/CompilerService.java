package com.snl.compiler.application;

import com.snl.compiler.api.dto.CompileRequest;
import com.snl.compiler.api.dto.CompileResponse;
import org.springframework.stereotype.Service;

@Service
public class CompilerService {
    private final CompilerPipeline pipeline;

    public CompilerService(CompilerPipeline pipeline) {
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

    public synchronized CompileResponse analyzeCodegen(CompileRequest request) {
        return pipeline.codegen(request);
    }
}
