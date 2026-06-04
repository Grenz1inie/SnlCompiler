package com.snl.compiler.api;

import com.snl.compiler.api.dto.CompileRequest;
import com.snl.compiler.api.dto.CompileResponse;
import com.snl.compiler.application.CompilerService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping(value = "/api/compile", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
public class CompilerController {
    private final CompilerService compilerService;

    public CompilerController(CompilerService compilerService) {
        this.compilerService = compilerService;
    }

    @PostMapping("/lexical")
    public CompileResponse lexical(@RequestBody CompileRequest request) {
        return compilerService.analyzeLexical(request);
    }

    @PostMapping("/grammar")
    public CompileResponse grammar(@RequestBody CompileRequest request) {
        return compilerService.analyzeGrammar(request);
    }

    @PostMapping("/recursive")
    public CompileResponse recursive(@RequestBody CompileRequest request) {
        return compilerService.analyzeRecursive(request);
    }

    @PostMapping("/semantic")
    public CompileResponse semantic(@RequestBody CompileRequest request) {
        return compilerService.analyzeSemantic(request);
    }

    @PostMapping("/codegen")
    public CompileResponse codegen(@RequestBody CompileRequest request) {
        return compilerService.analyzeCodegen(request);
    }
}
