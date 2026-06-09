package com.snl.compiler.application.codegen;

import com.snl.compiler.core.ast.BaseASTNode;
import com.snl.compiler.core.ast.BaseASTNode.ExpKind;
import com.snl.compiler.core.ast.BaseASTNode.NodeKind;
import com.snl.compiler.core.ast.BaseASTNode.StmKind;
import com.snl.compiler.core.ast.BaseASTNode.VarKind;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 中间代码生成器：遍历语义分析后的 AST，生成四元式 IR。
 * 处理声明、赋值、读写、过程调用及表达式（按 relExp/exp/term/factor 分层）。
 */
@Component
public class IrGenerator {
    private final CodegenSymbols symbols = new CodegenSymbols();
    private IrProgram program;

    /** 从程序根节点 ProK 生成 main 过程及全局数据区的 IR */
    IrProgram generate(BaseASTNode root) {
        symbols.reset();
        program = new IrProgram();

        if (root == null || root.nodeKind != NodeKind.ProK) {
            program.errors.add("无法生成中间代码：缺少程序根节点。");
            return program;
        }

        symbols.collectGlobalDeclarations(root.child[1], program);
        emitProcedures(root.child[1]);
        program.add(IrInstruction.label("main"));
        if (root.child[2] != null) {
            emitStatements(root.child[2].child[0]);
        }
        program.add(IrInstruction.exit());
        program.errors.addAll(symbols.errors);
        return program;
    }

    private void emitProcedures(BaseASTNode decls) {
        for (BaseASTNode node = decls; node != null; node = node.sibling) {
            if (node.nodeKind == NodeKind.ProcDecK) {
                emitProcedure(node);
            }
        }
    }

    private void emitProcedure(BaseASTNode proc) {
        String procName = proc.name.isEmpty() ? "proc" : proc.name.get(0);
        program.add(IrInstruction.label("_proc_" + CodegenSymbols.sanitize(procName)));
        symbols.procPrefix.push(CodegenSymbols.sanitize(procName) + "_");
        symbols.scopes.push(new LinkedHashMap<String, CodegenSymbols.VarSlot>());
        if (proc.child[1] != null) {
            for (BaseASTNode decl = proc.child[1]; decl != null; decl = decl.sibling) {
                if (decl.nodeKind == NodeKind.TypeK) {
                    symbols.registerType(decl);
                } else if (decl.nodeKind == NodeKind.VarK) {
                    symbols.registerVariables(decl, program);
                }
            }
        }
        bindParameters(proc.child[0]);
        if (proc.child[2] != null) {
            emitStatements(proc.child[2].child[0]);
        }
        symbols.scopes.pop();
        symbols.procPrefix.pop();
        program.add(IrInstruction.ret());
    }

    private void bindParameters(BaseASTNode params) {
        int arg = 0;
        for (BaseASTNode param = params; param != null; param = param.sibling) {
            if (param.nodeKind != NodeKind.VarK) {
                continue;
            }
            String paramTemp = symbols.freshTemp();
            program.add(IrInstruction.constVal(paramTemp, "$a" + arg));
            for (String name : param.name) {
                CodegenSymbols.VarSlot slot = symbols.lookupSlot(name);
                if (slot == null) {
                    program.errors.add("过程参数 '" + name + "' 未绑定存储。");
                    continue;
                }
                program.add(IrInstruction.store(slot.label, paramTemp));
            }
            arg++;
            if (arg > 3) {
                program.errors.add("当前后端最多支持 4 个参数。");
                break;
            }
        }
    }

    private void emitStatements(BaseASTNode list) {
        for (BaseASTNode stm = list; stm != null; stm = stm.sibling) {
            emitStatement(stm);
        }
    }

    private void emitStatement(BaseASTNode node) {
        if (node == null || node.stmKind == null) {
            return;
        }
        switch (node.stmKind) {
            case AssignK:
                storeVariable(node.child[0], evalExp(node.child[1]));
                break;
            case ReadK:
                if (!node.name.isEmpty()) {
                    CodegenSymbols.VarSlot slot = symbols.lookupSlot(node.name.get(0));
                    if (slot == null) {
                        program.errors.add("read 变量 '" + node.name.get(0) + "' 未声明。");
                    } else {
                        program.add(IrInstruction.read(slot.label));
                    }
                }
                break;
            case WriteK:
                program.add(IrInstruction.write(evalExp(node.child[0])));
                break;
            case IfK:
                emitIf(node);
                break;
            case WhileK:
                emitWhile(node);
                break;
            case CallK:
                emitCall(node);
                break;
            case ReturnK:
                program.add(IrInstruction.ret());
                break;
            default:
                program.errors.add("第" + node.lineno + "行：暂不支持的语句类型 " + node.stmKind);
                break;
        }
    }

    private void emitIf(BaseASTNode node) {
        String elseLabel = symbols.freshLabel("else");
        String endLabel = symbols.freshLabel("endif");
        emitCondBranch(node.child[0], elseLabel);
        emitStatements(node.child[1]);
        program.add(IrInstruction.goTo(endLabel));
        program.add(IrInstruction.label(elseLabel));
        emitStatements(node.child[2]);
        program.add(IrInstruction.label(endLabel));
    }

    private void emitWhile(BaseASTNode node) {
        String loopLabel = symbols.freshLabel("while");
        String endLabel = symbols.freshLabel("endwhile");
        program.add(IrInstruction.label(loopLabel));
        emitCondBranch(node.child[0], endLabel);
        emitStatements(node.child[1]);
        program.add(IrInstruction.goTo(loopLabel));
        program.add(IrInstruction.label(endLabel));
    }

    private void emitCondBranch(BaseASTNode cond, String falseLabel) {
        if (cond == null) {
            program.add(IrInstruction.goTo(falseLabel));
            return;
        }
        if (cond.expKind == ExpKind.OpK && cond.op != null) {
            String left = evalExp(cond.child[0]);
            String right = evalExp(cond.child[1]);
            String temp = symbols.freshTemp();
            program.add(IrInstruction.cmp(temp, cond.op, left, right));
            program.add(IrInstruction.ifFalse(temp, falseLabel));
            return;
        }
        String value = evalExp(cond);
        program.add(IrInstruction.ifFalse(value, falseLabel));
    }

    private void emitCall(BaseASTNode node) {
        String procName = node.name.isEmpty() ? "proc" : node.name.get(0);
        for (BaseASTNode param = node.child[0]; param != null; param = param.sibling) {
            program.add(IrInstruction.param(evalExp(param)));
        }
        program.add(IrInstruction.call("_proc_" + CodegenSymbols.sanitize(procName)));
    }

    private String evalExp(BaseASTNode node) {
        if (node == null) {
            return "0";
        }
        if (node.expKind == ExpKind.ConstK) {
            String temp = symbols.freshTemp();
            if (node.expType != null && "Char".equals(String.valueOf(node.expType))) {
                int ch = node.name.isEmpty() ? 0 : node.name.get(0).charAt(0);
                program.add(IrInstruction.constVal(temp, String.valueOf(ch)));
            } else {
                program.add(IrInstruction.constVal(temp, String.valueOf(node.val)));
            }
            return temp;
        }
        if (node.expKind == ExpKind.IdK) {
            return loadVariable(node);
        }
        if (node.expKind == ExpKind.OpK) {
            String left = evalExp(node.child[0]);
            String right = evalExp(node.child[1]);
            String target = symbols.freshTemp();
            program.add(IrInstruction.binop(target, node.op, left, right));
            return target;
        }
        return "0";
    }

    private String loadVariable(BaseASTNode node) {
        if (node.varKind == VarKind.ArrayV) {
            return loadArray(node);
        }
        if (node.varKind == VarKind.FieldV) {
            program.errors.add("第" + node.lineno + "行：记录域访问暂未生成中间代码。");
            return "0";
        }
        if (node.name.isEmpty()) {
            return "0";
        }
        CodegenSymbols.VarSlot slot = symbols.lookupSlot(node.name.get(0));
        if (slot == null) {
            program.errors.add("变量 '" + node.name.get(0) + "' 未声明。");
            return "0";
        }
        String temp = symbols.freshTemp();
        program.add(IrInstruction.load(temp, slot.label));
        return temp;
    }

    private String loadArray(BaseASTNode node) {
        CodegenSymbols.VarSlot slot = resolveArraySlot(node.child[0]);
        if (slot == null) {
            return "0";
        }
        String index = evalExp(node.child[1]);
        String offset = emitIndexOffset(index, slot.type.arrayLow);
        String temp = symbols.freshTemp();
        program.add(IrInstruction.loadIdx(temp, slot.label, offset));
        return temp;
    }

    private void storeVariable(BaseASTNode node, String value) {
        if (node == null) {
            return;
        }
        if (node.varKind == VarKind.ArrayV) {
            storeArray(node, value);
            return;
        }
        if (node.varKind == VarKind.FieldV) {
            program.errors.add("第" + node.lineno + "行：记录域赋值暂未生成中间代码。");
            return;
        }
        if (node.name.isEmpty()) {
            return;
        }
        CodegenSymbols.VarSlot slot = symbols.lookupSlot(node.name.get(0));
        if (slot == null) {
            program.errors.add("变量 '" + node.name.get(0) + "' 未声明。");
            return;
        }
        program.add(IrInstruction.store(slot.label, value));
    }

    private void storeArray(BaseASTNode node, String value) {
        CodegenSymbols.VarSlot slot = resolveArraySlot(node.child[0]);
        if (slot == null) {
            return;
        }
        String index = evalExp(node.child[1]);
        String offset = emitIndexOffset(index, slot.type.arrayLow);
        program.add(IrInstruction.storeIdx(slot.label, offset, value));
    }

    private String emitIndexOffset(String index, int low) {
        if (low == 0) {
            return index;
        }
        String lowTemp = symbols.freshTemp();
        program.add(IrInstruction.constVal(lowTemp, String.valueOf(low)));
        String offset = symbols.freshTemp();
        program.add(IrInstruction.binop(offset, "-", index, lowTemp));
        return offset;
    }

    private CodegenSymbols.VarSlot resolveArraySlot(BaseASTNode base) {
        while (base != null && base.varKind == VarKind.ArrayV) {
            base = base.child[0];
        }
        if (base == null || base.name.isEmpty()) {
            program.errors.add("数组基址变量无效。");
            return null;
        }
        return symbols.lookupSlot(base.name.get(0));
    }
}
