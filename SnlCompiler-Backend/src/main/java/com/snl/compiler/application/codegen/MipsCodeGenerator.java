package com.snl.compiler.application.codegen;

import com.snl.compiler.core.ast.BaseASTNode;
import com.snl.compiler.core.ast.BaseASTNode.DecKind;
import com.snl.compiler.core.ast.BaseASTNode.ExpKind;
import com.snl.compiler.core.ast.BaseASTNode.NodeKind;
import com.snl.compiler.core.ast.BaseASTNode.StmKind;
import com.snl.compiler.core.ast.BaseASTNode.VarKind;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MipsCodeGenerator {
    private final StringBuilder data = new StringBuilder();
    private final StringBuilder text = new StringBuilder();
    private final List<String> errors = new ArrayList<String>();
    private final Map<String, TypeInfo> namedTypes = new HashMap<String, TypeInfo>();
    private final Deque<Map<String, VarSlot>> scopes = new ArrayDeque<Map<String, VarSlot>>();
    private final Deque<String> procPrefix = new ArrayDeque<String>();
    private int labelCounter = 0;
    private int tempCounter = 0;

    public String generate(BaseASTNode root) {
        errors.clear();
        data.setLength(0);
        text.setLength(0);
        namedTypes.clear();
        scopes.clear();
        procPrefix.clear();
        labelCounter = 0;
        tempCounter = 0;
        scopes.push(new LinkedHashMap<String, VarSlot>());

        if (root == null || root.nodeKind != NodeKind.ProK) {
            errors.add("无法生成目标代码：缺少程序根节点。");
            return render(errors, "");
        }

        collectGlobalDeclarations(root.child[1]);
        emitProcedures(root.child[1]);
        emitLine(".text");
        emitLine(".globl main");
        emitLine("main:");
        if (root.child[2] != null) {
            emitStatements(root.child[2].child[0]);
        }
        emitLine("    li $v0, 10");
        emitLine("    syscall");
        return render(errors, "");
    }

    public List<String> getErrors() {
        return errors;
    }

    private void collectGlobalDeclarations(BaseASTNode decls) {
        for (BaseASTNode node = decls; node != null; node = node.sibling) {
            if (node.nodeKind == NodeKind.TypeK) {
                registerType(node);
            } else if (node.nodeKind == NodeKind.VarK) {
                registerVariables(node);
            }
        }
    }

    private void registerType(BaseASTNode node) {
        for (String name : node.name) {
            namedTypes.put(name, describeType(node.child[0]));
        }
    }

    private void registerVariables(BaseASTNode node) {
        TypeInfo type = describeType(node.child[0]);
        for (String name : node.name) {
            defineSlot(name, type, currentPrefix());
        }
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
        String label = "_proc_" + sanitize(procName);
        emitLine("");
        emitLine(label + ":");
        procPrefix.push(sanitize(procName) + "_");
        scopes.push(new LinkedHashMap<String, VarSlot>());
        if (proc.child[1] != null) {
            for (BaseASTNode decl = proc.child[1]; decl != null; decl = decl.sibling) {
                if (decl.nodeKind == NodeKind.TypeK) {
                    registerType(decl);
                } else if (decl.nodeKind == NodeKind.VarK) {
                    registerVariables(decl);
                }
            }
        }
        bindParameters(proc.child[0]);
        if (proc.child[2] != null) {
            emitStatements(proc.child[2].child[0]);
        }
        scopes.pop();
        procPrefix.pop();
        emitLine("    jr $ra");
    }

    private void bindParameters(BaseASTNode params) {
        int arg = 0;
        for (BaseASTNode param = params; param != null; param = param.sibling) {
            if (param.nodeKind != NodeKind.VarK) {
                continue;
            }
            String reg = "$a" + arg;
            for (String name : param.name) {
                VarSlot slot = lookupSlot(name);
                if (slot == null) {
                    errors.add("过程参数 '" + name + "' 未绑定存储。");
                    continue;
                }
                emitLine("    sw " + reg + ", " + slot.label);
            }
            arg++;
            if (arg > 3) {
                errors.add("当前 MIPS 后端最多支持 4 个参数。");
                break;
            }
        }
    }

    private void defineSlot(String name, TypeInfo type, String prefix) {
        Map<String, VarSlot> scope = scopes.peek();
        if (scope.containsKey(name)) {
            errors.add("变量 '" + name + "' 重复定义，已跳过。");
            return;
        }
        String label = prefix + sanitize(name);
        VarSlot slot = new VarSlot(label, type);
        scope.put(name, slot);
        emitData(slot);
    }

    private void emitData(VarSlot slot) {
        if (slot.type.kind == TypeKind.CHAR) {
            emitDataLine(label(slot) + ": .byte 0");
        } else if (slot.type.kind == TypeKind.INT) {
            emitDataLine(label(slot) + ": .word 0");
        } else if (slot.type.kind == TypeKind.ARRAY_INT) {
            int bytes = slot.type.elementCount * 4;
            emitDataLine(label(slot) + ": .space " + bytes);
        } else if (slot.type.kind == TypeKind.ARRAY_CHAR) {
            emitDataLine(label(slot) + ": .space " + slot.type.elementCount);
        } else {
            errors.add("暂不支持为类型 '" + slot.type.display + "' 分配存储。");
        }
    }

    private void emitDataLine(String line) {
        data.append(line).append('\n');
    }

    private String label(VarSlot slot) {
        return slot.label;
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
                    emitRead(node.name.get(0));
                }
                break;
            case WriteK:
                emitWrite(evalExp(node.child[0]));
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
                emitLine("    jr $ra");
                break;
            default:
                errors.add("第" + node.lineno + "行：暂不支持的语句类型 " + node.stmKind);
                break;
        }
    }

    private void emitRead(String name) {
        VarSlot slot = lookupSlot(name);
        if (slot == null) {
            errors.add("read 变量 '" + name + "' 未声明。");
            return;
        }
        emitLine("    li $v0, 5");
        emitLine("    syscall");
        if (slot.type.kind == TypeKind.CHAR) {
            emitLine("    sb $v0, " + slot.label);
        } else {
            emitLine("    sw $v0, " + slot.label);
        }
    }

    private void emitWrite(String reg) {
        emitLine("    move $a0, " + reg);
        emitLine("    li $v0, 1");
        emitLine("    syscall");
        emitLine("    li $a0, 10");
        emitLine("    li $v0, 11");
        emitLine("    syscall");
    }

    private void emitIf(BaseASTNode node) {
        String elseLabel = freshLabel("else");
        String endLabel = freshLabel("endif");
        emitCondBranch(node.child[0], elseLabel);
        emitStatements(node.child[1]);
        emitLine("    j " + endLabel);
        emitLine(elseLabel + ":");
        emitStatements(node.child[2]);
        emitLine(endLabel + ":");
    }

    private void emitWhile(BaseASTNode node) {
        String loopLabel = freshLabel("while");
        String endLabel = freshLabel("endwhile");
        emitLine(loopLabel + ":");
        emitCondBranch(node.child[0], endLabel);
        emitStatements(node.child[1]);
        emitLine("    j " + loopLabel);
        emitLine(endLabel + ":");
    }

    private void emitCondBranch(BaseASTNode cond, String falseLabel) {
        if (cond == null) {
            emitLine("    j " + falseLabel);
            return;
        }
        if (cond.expKind == ExpKind.OpK && cond.op != null) {
            String left = evalExp(cond.child[0]);
            String right = evalExp(cond.child[1]);
            String temp = freshTemp();
            if ("<".equals(cond.op)) {
                emitLine("    slt " + temp + ", " + left + ", " + right);
            } else if ("=".equals(cond.op)) {
                emitLine("    seq " + temp + ", " + left + ", " + right);
            } else {
                errors.add("不支持的关系运算符: " + cond.op);
                emitLine("    j " + falseLabel);
                return;
            }
            emitLine("    beq " + temp + ", $zero, " + falseLabel);
            return;
        }
        String value = evalExp(cond);
        emitLine("    beq " + value + ", $zero, " + falseLabel);
    }

    private void emitCall(BaseASTNode node) {
        String procName = node.name.isEmpty() ? "proc" : node.name.get(0);
        int arg = 0;
        for (BaseASTNode param = node.child[0]; param != null; param = param.sibling) {
            String reg = "$a" + arg;
            String value = evalExp(param);
            emitLine("    move " + reg + ", " + value);
            arg++;
        }
        emitLine("    jal _proc_" + sanitize(procName));
    }

    private String evalExp(BaseASTNode node) {
        if (node == null) {
            return "$zero";
        }
        if (node.expKind == ExpKind.ConstK) {
            String reg = freshTemp();
            if (node.expType != null && "Char".equals(String.valueOf(node.expType))) {
                int ch = node.name.isEmpty() ? 0 : node.name.get(0).charAt(0);
                emitLine("    li " + reg + ", " + ch);
            } else {
                emitLine("    li " + reg + ", " + node.val);
            }
            return reg;
        }
        if (node.expKind == ExpKind.IdK) {
            return loadVariable(node);
        }
        if (node.expKind == ExpKind.OpK) {
            String left = evalExp(node.child[0]);
            String right = evalExp(node.child[1]);
            String target = freshTemp();
            if ("+".equals(node.op)) {
                emitLine("    add " + target + ", " + left + ", " + right);
            } else if ("-".equals(node.op)) {
                emitLine("    sub " + target + ", " + left + ", " + right);
            } else if ("*".equals(node.op)) {
                emitLine("    mul " + target + ", " + left + ", " + right);
            } else if ("/".equals(node.op)) {
                emitLine("    div " + left + ", " + right);
                emitLine("    mflo " + target);
            } else {
                errors.add("不支持的运算符: " + node.op);
                return "$zero";
            }
            return target;
        }
        return "$zero";
    }

    private String loadVariable(BaseASTNode node) {
        if (node.varKind == VarKind.ArrayV) {
            return loadArray(node);
        }
        if (node.varKind == VarKind.FieldV) {
            errors.add("第" + node.lineno + "行：记录域访问暂未生成 MIPS。");
            return "$zero";
        }
        if (node.name.isEmpty()) {
            return "$zero";
        }
        VarSlot slot = lookupSlot(node.name.get(0));
        if (slot == null) {
            errors.add("变量 '" + node.name.get(0) + "' 未声明。");
            return "$zero";
        }
        String reg = freshTemp();
        if (slot.type.kind == TypeKind.CHAR || slot.type.kind == TypeKind.ARRAY_CHAR) {
            emitLine("    lb " + reg + ", " + slot.label);
        } else {
            emitLine("    lw " + reg + ", " + slot.label);
        }
        return reg;
    }

    private String loadArray(BaseASTNode node) {
        VarSlot slot = resolveArraySlot(node.child[0]);
        if (slot == null) {
            return "$zero";
        }
        String index = evalExp(node.child[1]);
        String addr = freshTemp();
        String scaled = freshTemp();
        String result = freshTemp();
        emitLine("    la " + addr + ", " + slot.label);
        if (slot.type.kind == TypeKind.ARRAY_INT) {
            emitIndexOffset(scaled, index, slot.type.arrayLow);
            emitLine("    sll " + scaled + ", " + scaled + ", 2");
            emitLine("    add " + addr + ", " + addr + ", " + scaled);
            emitLine("    lw " + result + ", 0(" + addr + ")");
        } else {
            emitIndexOffset(scaled, index, slot.type.arrayLow);
            emitLine("    add " + addr + ", " + addr + ", " + scaled);
            emitLine("    lb " + result + ", 0(" + addr + ")");
        }
        return result;
    }

    private void storeVariable(BaseASTNode node, String valueReg) {
        if (node == null) {
            return;
        }
        if (node.varKind == VarKind.ArrayV) {
            storeArray(node, valueReg);
            return;
        }
        if (node.varKind == VarKind.FieldV) {
            errors.add("第" + node.lineno + "行：记录域赋值暂未生成 MIPS。");
            return;
        }
        if (node.name.isEmpty()) {
            return;
        }
        VarSlot slot = lookupSlot(node.name.get(0));
        if (slot == null) {
            errors.add("变量 '" + node.name.get(0) + "' 未声明。");
            return;
        }
        if (slot.type.kind == TypeKind.CHAR) {
            emitLine("    sb " + valueReg + ", " + slot.label);
        } else {
            emitLine("    sw " + valueReg + ", " + slot.label);
        }
    }

    private void storeArray(BaseASTNode node, String valueReg) {
        VarSlot slot = resolveArraySlot(node.child[0]);
        if (slot == null) {
            return;
        }
        String index = evalExp(node.child[1]);
        String addr = freshTemp();
        String scaled = freshTemp();
        emitLine("    la " + addr + ", " + slot.label);
        if (slot.type.kind == TypeKind.ARRAY_INT) {
            emitIndexOffset(scaled, index, slot.type.arrayLow);
            emitLine("    sll " + scaled + ", " + scaled + ", 2");
            emitLine("    add " + addr + ", " + addr + ", " + scaled);
            emitLine("    sw " + valueReg + ", 0(" + addr + ")");
        } else {
            emitIndexOffset(scaled, index, slot.type.arrayLow);
            emitLine("    add " + addr + ", " + addr + ", " + scaled);
            emitLine("    sb " + valueReg + ", 0(" + addr + ")");
        }
    }

    private VarSlot resolveArraySlot(BaseASTNode base) {
        while (base != null && base.varKind == VarKind.ArrayV) {
            base = base.child[0];
        }
        if (base == null || base.name.isEmpty()) {
            errors.add("数组基址变量无效。");
            return null;
        }
        return lookupSlot(base.name.get(0));
    }

    private VarSlot lookupSlot(String name) {
        java.util.Iterator<Map<String, VarSlot>> it = scopes.descendingIterator();
        while (it.hasNext()) {
            VarSlot slot = it.next().get(name);
            if (slot != null) {
                return slot;
            }
        }
        return null;
    }

    private void emitIndexOffset(String dest, String indexReg, int low) {
        if (low == 0) {
            emitLine("    move " + dest + ", " + indexReg);
            return;
        }
        String lowReg = freshTemp();
        emitLine("    li " + lowReg + ", " + low);
        emitLine("    sub " + dest + ", " + indexReg + ", " + lowReg);
    }

    private TypeInfo describeType(BaseASTNode typeNode) {
        if (typeNode == null) {
            return TypeInfo.integer();
        }
        if (typeNode.decKind == DecKind.IntegerK) {
            return TypeInfo.integer();
        }
        if (typeNode.decKind == DecKind.CharK) {
            return TypeInfo.charType();
        }
        if (typeNode.decKind == DecKind.ArrayK) {
            TypeInfo element = describeType(typeNode.child[0]);
            int low = typeNode.arrayLow;
            int high = typeNode.arrayHigh;
            int count = Math.max(0, high - low + 1);
            if (element.kind == TypeKind.CHAR) {
                return TypeInfo.arrayChar(count, low);
            }
            return TypeInfo.arrayInt(count, low);
        }
        if (typeNode.decKind == DecKind.IdK && typeNode.typeName != null) {
            TypeInfo alias = namedTypes.get(typeNode.typeName);
            if (alias != null) {
                return alias;
            }
        }
        return TypeInfo.integer();
    }

    private String currentPrefix() {
        return procPrefix.isEmpty() ? "" : procPrefix.peek();
    }

    private String sanitize(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        if (sb.length() == 0) {
            return "tmp";
        }
        if (Character.isDigit(sb.charAt(0))) {
            sb.insert(0, '_');
        }
        return sb.toString();
    }

    private String freshLabel(String prefix) {
        return prefix + "_" + (labelCounter++);
    }

    private String freshTemp() {
        int idx = tempCounter % 8;
        tempCounter++;
        return "$t" + idx;
    }

    private void emitLine(String line) {
        text.append(line).append('\n');
    }

    private String render(List<String> genErrors, String ignored) {
        StringBuilder output = new StringBuilder();
        if (!genErrors.isEmpty()) {
            output.append("--- 代码生成警告/错误 ---\n");
            for (String error : genErrors) {
                output.append(error).append('\n');
            }
            output.append('\n');
        }
        output.append(".data\n");
        if (data.length() == 0) {
            output.append("    _snl_placeholder: .word 0\n");
        } else {
            output.append(data);
        }
        output.append('\n');
        output.append(text);
        return output.toString();
    }

    private enum TypeKind {
        INT, CHAR, ARRAY_INT, ARRAY_CHAR
    }

    private static final class TypeInfo {
        private final TypeKind kind;
        private final String display;
        private final int elementCount;
        private final int arrayLow;

        private TypeInfo(TypeKind kind, String display, int elementCount, int arrayLow) {
            this.kind = kind;
            this.display = display;
            this.elementCount = elementCount;
            this.arrayLow = arrayLow;
        }

        static TypeInfo integer() {
            return new TypeInfo(TypeKind.INT, "integer", 1, 0);
        }

        static TypeInfo charType() {
            return new TypeInfo(TypeKind.CHAR, "char", 1, 0);
        }

        static TypeInfo arrayInt(int count, int low) {
            return new TypeInfo(TypeKind.ARRAY_INT, "array of integer", count, low);
        }

        static TypeInfo arrayChar(int count, int low) {
            return new TypeInfo(TypeKind.ARRAY_CHAR, "array of char", count, low);
        }
    }

    private static final class VarSlot {
        private final String label;
        private final TypeInfo type;

        private VarSlot(String label, TypeInfo type) {
            this.label = label;
            this.type = type;
        }
    }
}
