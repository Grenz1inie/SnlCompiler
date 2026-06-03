package com.snl.compiler.controller;

import com.snl.compiler.service.CompileRequest;
import com.snl.compiler.service.CompileResponse;
import com.snl.compiler.service.CompilerService;

public class CompilerController {
    private final CompilerService compilerService;

    public CompilerController(CompilerService compilerService) {
        this.compilerService = compilerService;
    }

    public CompileResponse lexical(CompileRequest request) {
        return compilerService.analyzeLexical(request);
    }

    public CompileResponse grammar(CompileRequest request) {
        return compilerService.analyzeGrammar(request);
    }

    public CompileResponse recursive(CompileRequest request) {
        return compilerService.analyzeRecursive(request);
    }

    public CompileResponse semantic(CompileRequest request) {
        return compilerService.analyzeSemantic(request);
    }
}
