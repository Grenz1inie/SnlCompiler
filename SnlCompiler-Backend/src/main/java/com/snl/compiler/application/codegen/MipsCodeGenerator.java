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

    /**
     * 步骤 1：完整代码生成入口。
     * 1. AST 先转换成 IR。
     * 2. IR 再做中间代码优化。
     * 3. 优化后的 IR 翻译成 MIPS。
     * 4. 对 MIPS 做窥孔优化。
     * 5. 汇总四段输出与错误信息，返回结果对象。
     */
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

    /** 步骤 2：仅返回最终 MIPS 输出，内部仍会完整走一遍生成流水线。 */
    public String generate(BaseASTNode root) {
        return generateAll(root).mipsOutput;
    }

    /** 返回最近一次代码生成产生的错误信息。 */
    public List<String> getErrors() {
        if (lastResult == null) {
            return new java.util.ArrayList<String>();
        }
        return lastResult.errors;
    }
}
