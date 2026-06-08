package com.snl.compiler.application.codegen;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class IrOptimizer {
    IrProgram optimize(IrProgram source) {
        IrProgram program = source.copyProgram();
        foldConstants(program);
        simplifyAlgebra(program);
        propagateCopies(program);
        removeDeadTemps(program);
        return program;
    }

    private void foldConstants(IrProgram program) {
        Map<String, String> constMap = new HashMap<String, String>();
        List<IrInstruction> out = new ArrayList<IrInstruction>();
        for (IrInstruction ins : program.instructions) {
            if (ins.kind == IrInstruction.Kind.CONST) {
                constMap.put(ins.result, ins.arg1);
                out.add(ins);
                continue;
            }
            if (ins.kind == IrInstruction.Kind.BINOP || ins.kind == IrInstruction.Kind.CMP) {
                String left = resolve(constMap, ins.arg1);
                String right = resolve(constMap, ins.arg2);
                if (isInt(left) && isInt(right)) {
                    int a = Integer.parseInt(left);
                    int b = Integer.parseInt(right);
                    Integer folded = foldOp(ins.kind, ins.op, a, b);
                    if (folded != null) {
                        IrInstruction foldedIns = IrInstruction.constVal(ins.result, String.valueOf(folded));
                        constMap.put(ins.result, foldedIns.arg1);
                        out.add(foldedIns);
                        continue;
                    }
                }
            }
            out.add(rewriteOperands(ins, constMap));
            if (ins.result != null && ins.kind != IrInstruction.Kind.LABEL) {
                constMap.remove(ins.result);
            }
        }
        program.instructions.clear();
        program.instructions.addAll(out);
    }

    private void simplifyAlgebra(IrProgram program) {
        Map<String, String> constMap = new HashMap<String, String>();
        List<IrInstruction> out = new ArrayList<IrInstruction>();
        for (IrInstruction ins : program.instructions) {
            if (ins.kind == IrInstruction.Kind.CONST) {
                constMap.put(ins.result, ins.arg1);
                out.add(ins);
                continue;
            }
            if (ins.kind == IrInstruction.Kind.BINOP) {
                String left = resolve(constMap, ins.arg1);
                String right = resolve(constMap, ins.arg2);
                String simplified = simplifyBinop(ins.op, left, right);
                if (simplified != null) {
                    if (simplified.equals(left)) {
                        constMap.put(ins.result, left);
                        out.add(IrInstruction.constVal(ins.result, left));
                    } else {
                        out.add(IrInstruction.constVal(ins.result, simplified));
                        constMap.put(ins.result, simplified);
                    }
                    continue;
                }
            }
            out.add(rewriteOperands(ins, constMap));
            if (ins.result != null && ins.kind != IrInstruction.Kind.LABEL) {
                constMap.remove(ins.result);
            }
        }
        program.instructions.clear();
        program.instructions.addAll(out);
    }

    private void propagateCopies(IrProgram program) {
        Map<String, String> alias = new HashMap<String, String>();
        List<IrInstruction> out = new ArrayList<IrInstruction>();
        for (IrInstruction ins : program.instructions) {
            if (ins.kind == IrInstruction.Kind.CONST) {
                alias.put(ins.result, ins.arg1);
                out.add(ins);
                continue;
            }
            if (ins.kind == IrInstruction.Kind.LOAD) {
                alias.remove(ins.result);
                out.add(ins);
                continue;
            }
            IrInstruction rewritten = rewriteOperands(ins, alias);
            if (ins.kind == IrInstruction.Kind.BINOP || ins.kind == IrInstruction.Kind.CMP) {
                alias.remove(ins.result);
            }
            out.add(rewritten);
        }
        program.instructions.clear();
        program.instructions.addAll(out);
    }

    private void removeDeadTemps(IrProgram program) {
        Set<String> used = new HashSet<String>();
        for (IrInstruction ins : program.instructions) {
            collectUses(ins, used);
        }
        List<IrInstruction> out = new ArrayList<IrInstruction>();
        for (IrInstruction ins : program.instructions) {
            if (isDeadTempProducer(ins, used)) {
                continue;
            }
            out.add(ins);
        }
        program.instructions.clear();
        program.instructions.addAll(out);
    }

    private boolean isDeadTempProducer(IrInstruction ins, Set<String> used) {
        if (ins.result == null || ins.result.length() == 0) {
            return false;
        }
        if (!ins.result.startsWith("t")) {
            return false;
        }
        if (ins.kind == IrInstruction.Kind.STORE || ins.kind == IrInstruction.Kind.STORE_IDX
                || ins.kind == IrInstruction.Kind.READ || ins.kind == IrInstruction.Kind.WRITE
                || ins.kind == IrInstruction.Kind.PARAM || ins.kind == IrInstruction.Kind.CALL
                || ins.kind == IrInstruction.Kind.RETURN || ins.kind == IrInstruction.Kind.EXIT
                || ins.kind == IrInstruction.Kind.GOTO || ins.kind == IrInstruction.Kind.IF_FALSE
                || ins.kind == IrInstruction.Kind.LABEL) {
            return false;
        }
        return !used.contains(ins.result);
    }

    private void collectUses(IrInstruction ins, Set<String> used) {
        markUse(ins.arg1, used);
        markUse(ins.arg2, used);
        if (ins.kind == IrInstruction.Kind.STORE || ins.kind == IrInstruction.Kind.STORE_IDX) {
            markUse(ins.result, used);
        }
        if (ins.kind == IrInstruction.Kind.WRITE || ins.kind == IrInstruction.Kind.PARAM) {
            markUse(ins.arg1, used);
        }
    }

    private void markUse(String operand, Set<String> used) {
        if (operand != null && operand.startsWith("t")) {
            used.add(operand);
        }
    }

    private IrInstruction rewriteOperands(IrInstruction ins, Map<String, String> constMap) {
        IrInstruction copy = ins.copy();
        copy.arg1 = resolve(constMap, copy.arg1);
        copy.arg2 = resolve(constMap, copy.arg2);
        return copy;
    }

    private String resolve(Map<String, String> constMap, String operand) {
        if (operand == null) {
            return null;
        }
        String mapped = constMap.get(operand);
        return mapped != null ? mapped : operand;
    }

    private boolean isInt(String value) {
        if (value == null) {
            return false;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private Integer foldOp(IrInstruction.Kind kind, String op, int a, int b) {
        if (kind == IrInstruction.Kind.CMP) {
            if ("<".equals(op)) {
                return a < b ? 1 : 0;
            }
            if ("=".equals(op)) {
                return a == b ? 1 : 0;
            }
            return null;
        }
        if ("+".equals(op)) {
            return a + b;
        }
        if ("-".equals(op)) {
            return a - b;
        }
        if ("*".equals(op)) {
            return a * b;
        }
        if ("/".equals(op) && b != 0) {
            return a / b;
        }
        return null;
    }

    private String simplifyBinop(String op, String left, String right) {
        if ("+".equals(op) && "0".equals(right)) {
            return left;
        }
        if ("+".equals(op) && "0".equals(left)) {
            return right;
        }
        if ("-".equals(op) && "0".equals(right)) {
            return left;
        }
        if ("*".equals(op) && ("1".equals(left) || "1".equals(right))) {
            return "1".equals(left) ? right : left;
        }
        if ("*".equals(op) && ("0".equals(left) || "0".equals(right))) {
            return "0";
        }
        if ("/".equals(op) && "1".equals(right)) {
            return left;
        }
        return null;
    }
}
