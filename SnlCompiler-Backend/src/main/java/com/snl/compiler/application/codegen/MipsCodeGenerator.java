package com.snl.compiler.application.codegen;

import com.snl.compiler.core.ast.BaseASTNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 目标代码生成编排器：AST → IR → IR优化 → MIPS → 窥孔优化。
 * 输出四段文本：优化前 IR、优化后 IR、MIPS 直译、MIPS 窥孔后。
 */
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

    /** 完整代码生成流水线入口（语义分析通过后由 CompilerPipeline 调用） */
    public CodeGenResult generateAll(BaseASTNode root) {
        IrProgram ir = irGenerator.generate(root);           // AST → 四元式 IR
        IrProgram optimized = irOptimizer.optimize(ir);      // 常量折叠等四遍优化
        String mipsRaw = mipsEmitter.emit(optimized);        // IR → MIPS 汇编
        String mipsOpt = peepholeOptimizer.optimize(mipsRaw); // 窥孔优化

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
