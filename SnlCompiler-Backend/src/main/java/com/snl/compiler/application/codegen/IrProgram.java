package com.snl.compiler.application.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IrProgram {
    public final List<String> dataLines = new ArrayList<String>();
    public final List<IrInstruction> instructions = new ArrayList<IrInstruction>();
    public final List<String> errors = new ArrayList<String>();
    public final Map<String, CodegenSymbols.VarSlot> variables = new HashMap<String, CodegenSymbols.VarSlot>();

    public void add(IrInstruction instruction) {
        instructions.add(instruction);
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        if (!dataLines.isEmpty()) {
            sb.append("; --- 数据区 ---\n");
            for (String line : dataLines) {
                sb.append("; ").append(line).append('\n');
            }
            sb.append('\n');
        }
        int index = 0;
        for (IrInstruction ins : instructions) {
            if (ins.kind == IrInstruction.Kind.LABEL) {
                sb.append(ins.format(0)).append('\n');
            } else {
                index++;
                sb.append(ins.format(index)).append('\n');
            }
        }
        return sb.toString();
    }

    public IrProgram copyProgram() {
        IrProgram copy = new IrProgram();
        copy.dataLines.addAll(dataLines);
        copy.errors.addAll(errors);
        copy.variables.putAll(variables);
        for (IrInstruction ins : instructions) {
            copy.instructions.add(ins.copy());
        }
        return copy;
    }
}
