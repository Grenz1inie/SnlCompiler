package com.snl.compiler.model.symbol;

import java.util.ArrayList;
import java.util.List;

public class Symbol {
    public enum SymbolKind {
        TypeK, VarK, ProcK
    }

    public String name;
    public SymbolKind kind;
    public String typeName; // For variables and types
    public List<Symbol> params; // For procedures
    public int level; // Nesting level
    public int offset; // Memory offset (optional)
    
    // For arrays
    public int low;
    public int top;
    public String baseType;

    public Symbol(String name, SymbolKind kind) {
        this.name = name;
        this.kind = kind;
        this.params = new ArrayList<>();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-15s | %-10s | %-10s", name, kind, typeName != null ? typeName : ""));
        if (kind == SymbolKind.ProcK) {
            sb.append(" | Params: ").append(params.size());
        } else if (baseType != null) {
            sb.append(" | Array[").append(low).append("..").append(top).append("] of ").append(baseType);
        }
        return sb.toString();
    }
}
