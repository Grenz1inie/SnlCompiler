package com.snl.compiler.application.codegen;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MipsEmitter {
    private final Map<String, String> tempToReg = new HashMap<String, String>();
    private int regCounter;
    private int paramIndex;

    String emit(IrProgram program) {
        tempToReg.clear();
        regCounter = 0;
        paramIndex = 0;
        StringBuilder data = new StringBuilder();
        StringBuilder text = new StringBuilder();
        for (String line : program.dataLines) {
            data.append("    ").append(line).append('\n');
        }
        for (IrInstruction ins : program.instructions) {
            if (ins.kind == IrInstruction.Kind.LABEL) {
                text.append(ins.label).append(":\n");
                continue;
            }
            if (ins.kind == IrInstruction.Kind.CALL) {
                paramIndex = 0;
            }
            emitInstruction(ins, text, program);
        }
        StringBuilder output = new StringBuilder();
        output.append(".data\n");
        if (data.length() == 0) {
            output.append("    _snl_placeholder: .word 0\n");
        } else {
            output.append(data);
        }
        output.append('\n');
        output.append(".text\n");
        output.append(".globl main\n");
        output.append(text);
        return output.toString();
    }

    private void emitInstruction(IrInstruction ins, StringBuilder text, IrProgram program) {
        switch (ins.kind) {
            case CONST:
                emitLine(text, "li " + regOf(ins.result) + ", " + ins.arg1);
                break;
            case LOAD:
                emitLoad(text, ins.result, ins.arg1, program);
                break;
            case LOAD_IDX:
                emitLoadIdx(text, ins.result, ins.arg1, ins.arg2, program);
                break;
            case STORE:
                emitStore(text, ins.arg1, ins.result, program);
                break;
            case STORE_IDX:
                emitStoreIdx(text, ins.arg1, ins.arg2, ins.result, program);
                break;
            case BINOP:
                emitBinop(ins, text);
                break;
            case CMP:
                emitCmp(ins, text);
                break;
            case GOTO:
                emitLine(text, "j " + ins.label);
                break;
            case IF_FALSE:
                emitLine(text, "beq " + regOf(ins.arg1) + ", $zero, " + ins.label);
                break;
            case READ:
                emitRead(text, ins.arg1, program);
                break;
            case WRITE:
                emitWrite(text, ins.arg1);
                break;
            case PARAM:
                emitLine(text, "move $a" + paramIndex + ", " + materialize(ins.arg1, text));
                paramIndex++;
                break;
            case CALL:
                emitLine(text, "jal " + ins.arg1);
                paramIndex = 0;
                break;
            case RETURN:
                emitLine(text, "jr $ra");
                break;
            case EXIT:
                emitLine(text, "li $v0, 10");
                emitLine(text, "syscall");
                break;
            default:
                break;
        }
    }

    private void emitBinop(IrInstruction ins, StringBuilder text) {
        String left = materialize(ins.arg1, text);
        String right = materialize(ins.arg2, text);
        String dest = regOf(ins.result);
        if ("+".equals(ins.op)) {
            emitLine(text, "add " + dest + ", " + left + ", " + right);
        } else if ("-".equals(ins.op)) {
            emitLine(text, "sub " + dest + ", " + left + ", " + right);
        } else if ("*".equals(ins.op)) {
            emitLine(text, "mul " + dest + ", " + left + ", " + right);
        } else if ("/".equals(ins.op)) {
            emitLine(text, "div " + left + ", " + right);
            emitLine(text, "mflo " + dest);
        }
    }

    private void emitCmp(IrInstruction ins, StringBuilder text) {
        String left = materialize(ins.arg1, text);
        String right = materialize(ins.arg2, text);
        String dest = regOf(ins.result);
        if ("<".equals(ins.op)) {
            emitLine(text, "slt " + dest + ", " + left + ", " + right);
        } else if ("=".equals(ins.op)) {
            emitLine(text, "seq " + dest + ", " + left + ", " + right);
        }
    }

    private void emitLoad(StringBuilder text, String result, String var, IrProgram program) {
        CodegenSymbols.VarSlot slot = program.variables.get(var);
        if (slot != null && slot.type.kind == CodegenSymbols.TypeKind.CHAR) {
            emitLine(text, "lb " + regOf(result) + ", " + var);
        } else {
            emitLine(text, "lw " + regOf(result) + ", " + var);
        }
    }

    private void emitStore(StringBuilder text, String var, String value, IrProgram program) {
        CodegenSymbols.VarSlot slot = program.variables.get(var);
        if (slot != null && slot.type.kind == CodegenSymbols.TypeKind.CHAR) {
            emitLine(text, "sb " + materialize(value, text) + ", " + var);
        } else {
            emitLine(text, "sw " + materialize(value, text) + ", " + var);
        }
    }

    private void emitLoadIdx(StringBuilder text, String result, String array, String index, IrProgram program) {
        CodegenSymbols.VarSlot slot = program.variables.get(array);
        String addr = freshReg();
        String scaled = freshReg();
        emitLine(text, "la " + addr + ", " + array);
        if (slot != null && slot.type.kind == CodegenSymbols.TypeKind.ARRAY_INT) {
            emitLine(text, "sll " + scaled + ", " + materialize(index, text) + ", 2");
            emitLine(text, "add " + addr + ", " + addr + ", " + scaled);
            emitLine(text, "lw " + regOf(result) + ", 0(" + addr + ")");
        } else {
            emitLine(text, "add " + addr + ", " + addr + ", " + materialize(index, text));
            emitLine(text, "lb " + regOf(result) + ", 0(" + addr + ")");
        }
    }

    private void emitStoreIdx(StringBuilder text, String array, String index, String value, IrProgram program) {
        CodegenSymbols.VarSlot slot = program.variables.get(array);
        String addr = freshReg();
        String scaled = freshReg();
        emitLine(text, "la " + addr + ", " + array);
        if (slot != null && slot.type.kind == CodegenSymbols.TypeKind.ARRAY_INT) {
            emitLine(text, "sll " + scaled + ", " + materialize(index, text) + ", 2");
            emitLine(text, "add " + addr + ", " + addr + ", " + scaled);
            emitLine(text, "sw " + materialize(value, text) + ", 0(" + addr + ")");
        } else {
            emitLine(text, "add " + addr + ", " + addr + ", " + materialize(index, text));
            emitLine(text, "sb " + materialize(value, text) + ", 0(" + addr + ")");
        }
    }

    private void emitRead(StringBuilder text, String var, IrProgram program) {
        emitLine(text, "li $v0, 5");
        emitLine(text, "syscall");
        CodegenSymbols.VarSlot slot = program.variables.get(var);
        if (slot != null && slot.type.kind == CodegenSymbols.TypeKind.CHAR) {
            emitLine(text, "sb $v0, " + var);
        } else {
            emitLine(text, "sw $v0, " + var);
        }
    }

    private void emitWrite(StringBuilder text, String value) {
        emitLine(text, "move $a0, " + materialize(value, text));
        emitLine(text, "li $v0, 1");
        emitLine(text, "syscall");
        emitLine(text, "li $a0, 10");
        emitLine(text, "li $v0, 11");
        emitLine(text, "syscall");
    }

    private String materialize(String name, StringBuilder text) {
        if (name == null) {
            return "$zero";
        }
        if (name.startsWith("$")) {
            return name;
        }
        if (name.startsWith("t")) {
            return regOf(name);
        }
        if (isInt(name)) {
            String reg = freshReg();
            emitLine(text, "li " + reg + ", " + name);
            return reg;
        }
        return regOf(name);
    }

    private String regOf(String irTemp) {
        if (irTemp == null) {
            return "$zero";
        }
        if (irTemp.startsWith("$")) {
            return irTemp;
        }
        if (!tempToReg.containsKey(irTemp)) {
            tempToReg.put(irTemp, "$t" + (regCounter % 8));
            regCounter++;
        }
        return tempToReg.get(irTemp);
    }

    private String freshReg() {
        String reg = "$t" + (regCounter % 8);
        regCounter++;
        return reg;
    }

    private boolean isInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void emitLine(StringBuilder text, String line) {
        text.append("    ").append(line).append('\n');
    }
}
