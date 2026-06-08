package com.snl.compiler.application.codegen;

import com.snl.compiler.core.ast.BaseASTNode;
import com.snl.compiler.core.ast.BaseASTNode.DecKind;
import com.snl.compiler.core.ast.BaseASTNode.NodeKind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class CodegenSymbols {
    enum TypeKind {
        INT, CHAR, ARRAY_INT, ARRAY_CHAR
    }

    static final class TypeInfo {
        final TypeKind kind;
        final String display;
        final int elementCount;
        final int arrayLow;

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

    static final class VarSlot {
        final String label;
        final TypeInfo type;

        VarSlot(String label, TypeInfo type) {
            this.label = label;
            this.type = type;
        }
    }

    final List<String> errors = new ArrayList<String>();
    final Map<String, TypeInfo> namedTypes = new HashMap<String, TypeInfo>();
    final Deque<Map<String, VarSlot>> scopes = new ArrayDeque<Map<String, VarSlot>>();
    final Deque<String> procPrefix = new ArrayDeque<String>();
    int tempCounter;
    int labelCounter;

    void reset() {
        errors.clear();
        namedTypes.clear();
        scopes.clear();
        procPrefix.clear();
        tempCounter = 0;
        labelCounter = 0;
        scopes.push(new LinkedHashMap<String, VarSlot>());
    }

    String freshTemp() {
        return "t" + (tempCounter++);
    }

    String freshLabel(String prefix) {
        return prefix + "_" + (labelCounter++);
    }

    String currentPrefix() {
        return procPrefix.isEmpty() ? "" : procPrefix.peek();
    }

    void collectGlobalDeclarations(BaseASTNode decls, IrProgram program) {
        for (BaseASTNode node = decls; node != null; node = node.sibling) {
            if (node.nodeKind == NodeKind.TypeK) {
                registerType(node);
            } else if (node.nodeKind == NodeKind.VarK) {
                registerVariables(node, program);
            }
        }
    }

    void registerType(BaseASTNode node) {
        for (String name : node.name) {
            namedTypes.put(name, describeType(node.child[0]));
        }
    }

    void registerVariables(BaseASTNode node, IrProgram program) {
        TypeInfo type = describeType(node.child[0]);
        for (String name : node.name) {
            defineSlot(name, type, currentPrefix(), program);
        }
    }

    void defineSlot(String name, TypeInfo type, String prefix, IrProgram program) {
        Map<String, VarSlot> scope = scopes.peek();
        if (scope.containsKey(name)) {
            errors.add("变量 '" + name + "' 重复定义，已跳过。");
            return;
        }
        String label = prefix + sanitize(name);
        VarSlot slot = new VarSlot(label, type);
        scope.put(name, slot);
        program.variables.put(label, slot);
        emitData(slot, program);
    }

    void emitData(VarSlot slot, IrProgram program) {
        if (slot.type.kind == TypeKind.CHAR) {
            program.dataLines.add(slot.label + ": .byte 0");
        } else if (slot.type.kind == TypeKind.INT) {
            program.dataLines.add(slot.label + ": .word 0");
        } else if (slot.type.kind == TypeKind.ARRAY_INT) {
            program.dataLines.add(slot.label + ": .space " + (slot.type.elementCount * 4));
        } else if (slot.type.kind == TypeKind.ARRAY_CHAR) {
            program.dataLines.add(slot.label + ": .space " + slot.type.elementCount);
        } else {
            errors.add("暂不支持为类型 '" + slot.type.display + "' 分配存储。");
        }
    }

    VarSlot lookupSlot(String name) {
        java.util.Iterator<Map<String, VarSlot>> it = scopes.descendingIterator();
        while (it.hasNext()) {
            VarSlot slot = it.next().get(name);
            if (slot != null) {
                return slot;
            }
        }
        return null;
    }

    VarSlot lookupByLabel(String label) {
        for (Map<String, VarSlot> scope : scopes) {
            for (VarSlot slot : scope.values()) {
                if (slot.label.equals(label)) {
                    return slot;
                }
            }
        }
        return null;
    }

    TypeInfo describeType(BaseASTNode typeNode) {
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

    static String sanitize(String name) {
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
}
