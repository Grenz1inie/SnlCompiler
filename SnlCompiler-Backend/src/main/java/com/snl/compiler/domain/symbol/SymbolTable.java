package com.snl.compiler.domain.symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {
    private List<Map<String, Symbol>> scopes;
    private int currentLevel;

    public SymbolTable() {
        scopes = new ArrayList<>();
        scopes.add(new HashMap<>()); // Global scope
        currentLevel = 0;
    }

    public void enterScope() {
        scopes.add(new HashMap<>());
        currentLevel++;
    }

    public void exitScope() {
        if (currentLevel > 0) {
            scopes.remove(currentLevel);
            currentLevel--;
        }
    }

    public boolean insert(Symbol symbol) {
        if (scopes.get(currentLevel).containsKey(symbol.name)) {
            return false; // Already declared in current scope
        }
        symbol.level = currentLevel;
        scopes.get(currentLevel).put(symbol.name, symbol);
        return true;
    }

    public Symbol lookup(String name) {
        for (int i = currentLevel; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                return scopes.get(i).get(name);
            }
        }
        return null;
    }

    public List<Symbol> getAllSymbols() {
        List<Symbol> all = new ArrayList<>();
        for (Map<String, Symbol> scope : scopes) {
            all.addAll(scope.values());
        }
        return all;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-15s | %-10s | %-10s | Extra\n", "Name", "Kind", "Type"));
        sb.append("------------------------------------------------------------\n");
        for (int i = 0; i <= currentLevel; i++) {
            sb.append("Scope Level ").append(i).append(":\n");
            for (Symbol s : scopes.get(i).values()) {
                sb.append(s.toString()).append("\n");
            }
        }
        return sb.toString();
    }
}


