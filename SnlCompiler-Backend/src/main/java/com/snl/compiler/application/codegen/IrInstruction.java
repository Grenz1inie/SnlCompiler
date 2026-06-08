package com.snl.compiler.application.codegen;

public class IrInstruction {
    public enum Kind {
        LABEL,
        CONST,
        LOAD,
        LOAD_IDX,
        STORE,
        STORE_IDX,
        BINOP,
        CMP,
        GOTO,
        IF_FALSE,
        READ,
        WRITE,
        PARAM,
        CALL,
        RETURN,
        EXIT
    }

    public Kind kind;
    public String op;
    public String arg1;
    public String arg2;
    public String result;
    public String label;

    public static IrInstruction label(String name) {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.LABEL;
        ins.label = name;
        return ins;
    }

    public static IrInstruction constVal(String result, String value) {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.CONST;
        ins.arg1 = value;
        ins.result = result;
        return ins;
    }

    public static IrInstruction load(String result, String var) {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.LOAD;
        ins.arg1 = var;
        ins.result = result;
        return ins;
    }

    public static IrInstruction loadIdx(String result, String array, String index) {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.LOAD_IDX;
        ins.arg1 = array;
        ins.arg2 = index;
        ins.result = result;
        return ins;
    }

    public static IrInstruction store(String var, String value) {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.STORE;
        ins.arg1 = var;
        ins.result = value;
        return ins;
    }

    public static IrInstruction storeIdx(String array, String index, String value) {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.STORE_IDX;
        ins.arg1 = array;
        ins.arg2 = index;
        ins.result = value;
        return ins;
    }

    public static IrInstruction binop(String result, String op, String left, String right) {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.BINOP;
        ins.op = op;
        ins.arg1 = left;
        ins.arg2 = right;
        ins.result = result;
        return ins;
    }

    public static IrInstruction cmp(String result, String rel, String left, String right) {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.CMP;
        ins.op = rel;
        ins.arg1 = left;
        ins.arg2 = right;
        ins.result = result;
        return ins;
    }

    public static IrInstruction goTo(String label) {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.GOTO;
        ins.label = label;
        return ins;
    }

    public static IrInstruction ifFalse(String cond, String label) {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.IF_FALSE;
        ins.arg1 = cond;
        ins.label = label;
        return ins;
    }

    public static IrInstruction read(String var) {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.READ;
        ins.arg1 = var;
        return ins;
    }

    public static IrInstruction write(String value) {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.WRITE;
        ins.arg1 = value;
        return ins;
    }

    public static IrInstruction param(String value) {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.PARAM;
        ins.arg1 = value;
        return ins;
    }

    public static IrInstruction call(String proc) {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.CALL;
        ins.arg1 = proc;
        return ins;
    }

    public static IrInstruction ret() {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.RETURN;
        return ins;
    }

    public static IrInstruction exit() {
        IrInstruction ins = new IrInstruction();
        ins.kind = Kind.EXIT;
        return ins;
    }

    public String format(int index) {
        if (kind == Kind.LABEL) {
            return label + ":";
        }
        return String.format("%4d: (%s, %s, %s, %s)", index, formatOp(), nz(arg1), nz(arg2), nz(resultLabel()));
    }

    private String formatOp() {
        switch (kind) {
            case CONST: return "const";
            case LOAD: return "load";
            case LOAD_IDX: return "load[]";
            case STORE: return "store";
            case STORE_IDX: return "store[]";
            case BINOP: return op;
            case CMP: return op;
            case GOTO: return "goto";
            case IF_FALSE: return "if_false";
            case READ: return "read";
            case WRITE: return "write";
            case PARAM: return "param";
            case CALL: return "call";
            case RETURN: return "return";
            case EXIT: return "exit";
            default: return kind.name();
        }
    }

    private String resultLabel() {
        if (kind == Kind.GOTO || kind == Kind.IF_FALSE) {
            return label;
        }
        if (kind == Kind.STORE || kind == Kind.STORE_IDX) {
            return result;
        }
        if (kind == Kind.READ || kind == Kind.WRITE || kind == Kind.PARAM || kind == Kind.CALL || kind == Kind.RETURN || kind == Kind.EXIT) {
            return "-";
        }
        return result;
    }

    private static String nz(String value) {
        return value == null || value.length() == 0 ? "-" : value;
    }

    public IrInstruction copy() {
        IrInstruction ins = new IrInstruction();
        ins.kind = kind;
        ins.op = op;
        ins.arg1 = arg1;
        ins.arg2 = arg2;
        ins.result = result;
        ins.label = label;
        return ins;
    }
}
