package com.snl.compiler.core.ast;

import java.util.ArrayList;
import java.util.List;

public class BaseASTNode {
    public enum NodeKind {
        ProK, PheadK, TypeK, VarK, ProcDecK, StmK, ExpK
    }

    public enum DecKind {
        ArrayK, CharK, IntegerK, RecordK, IdK
    }

    public enum StmKind {
        IfK, WhileK, AssignK, ReadK, WriteK, CallK, ReturnK
    }

    public enum ExpKind {
        OpK, ConstK, IdK
    }

    public enum VarKind {
        IdV, ArrayV, FieldV
    }

    public enum ExpType {
        Void, Integer, Boolean, Char
    }

    public BaseASTNode[] child = new BaseASTNode[3];
    public BaseASTNode sibling;
    public int lineno;
    public NodeKind nodeKind;
    
    // For declarations
    public DecKind decKind;
    
    // For statements
    public StmKind stmKind;
    
    // For expressions
    public ExpKind expKind;
    public VarKind varKind;
    public ExpType expType;
    
    // For operators
    public String op;
    
    // For constants
    public int val;
    
    // For identifiers
    public List<String> name = new ArrayList<>();
    
    // For types
    public String typeName;
    public int arrayLow;
    public int arrayHigh;

    public BaseASTNode() {
        for (int i = 0; i < 3; i++) {
            child[i] = null;
        }
        sibling = null;
    }
}
