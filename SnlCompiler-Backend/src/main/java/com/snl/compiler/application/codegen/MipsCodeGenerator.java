package com.snl.compiler.application.codegen;

import com.snl.compiler.core.ast.BaseASTNode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MipsCodeGenerator {
    private final IrGenerator irGenerator;
    private final IrOptimizer irOptimizer;
    private final MipsEmitter mipsEmitter;
    private final MipsPeepholeOptimizer peepholeOptimizer;
    private CodeGenResult lastResult;

    public MipsCodeGenerator(
            IrGenerator irGenerator,
            IrOptimizer irOptimizer,
            MipsEmitter mipsEmitter,
            MipsPeepholeOptimizer peepholeOptimizer) {
        this.irGenerator = irGenerator;
        this.irOptimizer = irOptimizer;
        this.mipsEmitter = mipsEmitter;
        this.peepholeOptimizer = peepholeOptimizer;
    }

    public CodeGenResult generateAll(BaseASTNode root) {
        IrProgram ir = irGenerator.generate(root);
        IrProgram optimized = irOptimizer.optimize(ir);
        String mipsRaw = mipsEmitter.emit(optimized);
        String mipsOpt = peepholeOptimizer.optimize(mipsRaw);

        CodeGenResult result = new CodeGenResult();
        result.irOutput = ir.format();
        result.irOptimizedOutput = optimized.format();
        result.mipsRawOutput = mipsRaw;
        result.mipsOutput = mipsOpt;
        result.errors.addAll(ir.errors);
        lastResult = result;
        return result;
    }

    public String generate(BaseASTNode root) {
        return generateAll(root).mipsOutput;
    }

    public List<String> getErrors() {
        if (lastResult == null) {
            return new java.util.ArrayList<String>();
        }
        return lastResult.errors;
    }
}
