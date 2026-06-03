package com.snl.compiler.core.semantic;

import java.util.ArrayList;
import java.util.List;

import com.snl.compiler.core.ast.BaseASTNode;
import com.snl.compiler.core.ast.BaseASTNode.DecKind;
import com.snl.compiler.core.ast.BaseASTNode.ExpType;
import com.snl.compiler.core.ast.BaseASTNode.VarKind;
import com.snl.compiler.model.symbol.Symbol;
import com.snl.compiler.model.symbol.Symbol.SymbolKind;
import com.snl.compiler.model.symbol.SymbolTable;

public class SemanticAnalyzer {
    private SymbolTable symbolTable;
    private List<String> errors;

    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
        this.errors = new ArrayList<>();
    }

    public void analyze(BaseASTNode root) {
        if (root == null) {
            return;
        }
        traverse(root);
    }

    public List<String> getErrors() {
        return errors;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    private void traverse(BaseASTNode node) {
        if (node == null) {
            return;
        }

        switch (node.nodeKind) {
            case ProK:
                traverse(node.child[0]);
                traverse(node.child[1]);
                traverse(node.child[2]);
                break;
            case PheadK:
                break;
            case TypeK:
                handleTypeDec(node);
                break;
            case VarK:
                handleVarDec(node);
                break;
            case ProcDecK:
                handleProcDec(node);
                break;
            case StmK:
                if (node.stmKind == null) {
                    traverse(node.child[0]);
                } else {
                    handleStm(node);
                }
                break;
            case ExpK:
                handleExp(node);
                break;
            default:
                break;
        }

        if (node.sibling != null) {
            traverse(node.sibling);
        }
    }

    private String resolveTypeName(BaseASTNode typeNode) {
        if (typeNode == null) {
            return "unknown";
        }
        if (typeNode.decKind == DecKind.IntegerK) {
            return "integer";
        }
        if (typeNode.decKind == DecKind.CharK) {
            return "char";
        }
        if (typeNode.decKind == DecKind.ArrayK) {
            return "array";
        }
        if (typeNode.decKind == DecKind.RecordK) {
            return "record";
        }
        if (typeNode.decKind == DecKind.IdK && typeNode.typeName != null) {
            Symbol typeSym = symbolTable.lookup(typeNode.typeName);
            if (typeSym != null && typeSym.kind == SymbolKind.TypeK) {
                return typeSym.typeName != null ? typeSym.typeName : typeNode.typeName;
            }
            return typeNode.typeName;
        }
        return "unknown";
    }

    private void handleTypeDec(BaseASTNode node) {
        for (String name : node.name) {
            Symbol s = new Symbol(name, SymbolKind.TypeK);
            s.typeName = resolveTypeName(node.child[0]);
            if (node.child[0] != null && node.child[0].decKind == DecKind.ArrayK) {
                s.low = node.child[0].arrayLow;
                s.top = node.child[0].arrayHigh;
                s.baseType = resolveTypeName(node.child[0].child[0]);
            }
            if (!symbolTable.insert(s)) {
                errors.add("第" + node.lineno + "行：类型 '" + name + "' 重复定义");
            }
        }
    }

    private void handleVarDec(BaseASTNode node) {
        String typeName = resolveTypeName(node.child[0]);
        for (String name : node.name) {
            Symbol s = new Symbol(name, SymbolKind.VarK);
            s.typeName = typeName;
            if (node.child[0] != null && node.child[0].decKind == DecKind.ArrayK) {
                s.low = node.child[0].arrayLow;
                s.top = node.child[0].arrayHigh;
                s.baseType = resolveTypeName(node.child[0].child[0]);
            }
            if (!symbolTable.insert(s)) {
                errors.add("第" + node.lineno + "行：变量 '" + name + "' 重复定义");
            }
        }
    }

    private void handleProcDec(BaseASTNode node) {
        String procName = node.name.get(0);
        Symbol s = new Symbol(procName, SymbolKind.ProcK);
        if (!symbolTable.insert(s)) {
            errors.add("第" + node.lineno + "行：过程 '" + procName + "' 重复定义");
        }

        symbolTable.enterScope();
        traverse(node.child[0]);
        traverse(node.child[1]);
        traverse(node.child[2]);
        symbolTable.exitScope();
    }

    private void handleStm(BaseASTNode node) {
        if (node.stmKind == null) {
            return;
        }
        switch (node.stmKind) {
            case AssignK:
                checkAssignable(node.child[0], node.lineno);
                ExpType rhs = evalExpType(node.child[1]);
                if (node.child[0] != null && node.child[0].name.size() > 0) {
                    Symbol lhsSym = symbolTable.lookup(node.child[0].name.get(0));
                    if (lhsSym != null && rhs != ExpType.Void && !typesCompatible(lhsSym.typeName, rhs)) {
                        errors.add("第" + node.lineno + "行：赋值语句左右类型不兼容");
                    }
                }
                break;
            case IfK:
            case WhileK:
                ExpType cond = evalExpType(node.child[0]);
                if (cond != ExpType.Boolean && cond != ExpType.Void) {
                    errors.add("第" + node.lineno + "行：条件表达式应为关系表达式");
                }
                traverse(node.child[1]);
                traverse(node.child[2]);
                break;
            case CallK:
                String procName = node.name.get(0);
                Symbol proc = symbolTable.lookup(procName);
                if (proc == null) {
                    errors.add("第" + node.lineno + "行：过程 '" + procName + "' 未声明");
                } else if (proc.kind != SymbolKind.ProcK) {
                    errors.add("第" + node.lineno + "行：'" + procName + "' 不是过程名");
                }
                traverse(node.child[0]);
                break;
            case ReadK:
                checkVarIdentifier(node.name.get(0), node.lineno);
                break;
            case WriteK:
            case ReturnK:
                traverse(node.child[0]);
                break;
            default:
                break;
        }
    }

    private void checkAssignable(BaseASTNode varNode, int line) {
        if (varNode == null) {
            return;
        }
        if (varNode.varKind == VarKind.ArrayV || varNode.varKind == VarKind.FieldV) {
            checkAssignable(varNode.child[0], line);
            return;
        }
        if (varNode.name.isEmpty()) {
            return;
        }
        Symbol sym = symbolTable.lookup(varNode.name.get(0));
        if (sym == null) {
            errors.add("第" + line + "行：变量 '" + varNode.name.get(0) + "' 未声明");
        } else if (sym.kind == SymbolKind.TypeK) {
            errors.add("第" + line + "行：'" + varNode.name.get(0) + "' 是类型名，不能赋值");
        } else if (sym.kind == SymbolKind.ProcK) {
            errors.add("第" + line + "行：'" + varNode.name.get(0) + "' 是过程名，不能赋值");
        }
    }

    private void checkVarIdentifier(String name, int line) {
        Symbol sym = symbolTable.lookup(name);
        if (sym == null) {
            errors.add("第" + line + "行：变量 '" + name + "' 未声明");
        } else if (sym.kind != SymbolKind.VarK) {
            errors.add("第" + line + "行：'" + name + "' 不是变量标识符");
        }
    }

    private void handleExp(BaseASTNode node) {
        evalExpType(node);
    }

    private ExpType evalExpType(BaseASTNode node) {
        if (node == null) {
            return ExpType.Void;
        }
        if (node.expKind == null) {
            return ExpType.Void;
        }
        switch (node.expKind) {
            case ConstK:
                if (node.expType == ExpType.Char) {
                    return ExpType.Char;
                }
                node.expType = ExpType.Integer;
                return ExpType.Integer;
            case IdK:
                if (node.varKind == VarKind.ArrayV || node.varKind == VarKind.FieldV) {
                    evalExpType(node.child[0]);
                    if (node.child[1] != null) {
                        evalExpType(node.child[1]);
                    }
                    node.expType = ExpType.Integer;
                    return ExpType.Integer;
                }
                if (node.name.isEmpty()) {
                    return ExpType.Void;
                }
                Symbol sym = symbolTable.lookup(node.name.get(0));
                if (sym == null) {
                    errors.add("第" + node.lineno + "行：标识符 '" + node.name.get(0) + "' 未声明");
                    return ExpType.Void;
                }
                if (sym.kind == SymbolKind.TypeK) {
                    errors.add("第" + node.lineno + "行：'" + node.name.get(0) + "' 是类型名，不能作为表达式使用");
                    return ExpType.Void;
                }
                if (sym.kind == SymbolKind.ProcK) {
                    errors.add("第" + node.lineno + "行：'" + node.name.get(0) + "' 是过程名，不能作为表达式使用");
                    return ExpType.Void;
                }
                node.expType = toExpType(sym.typeName);
                return node.expType;
            case OpK:
                ExpType left = evalExpType(node.child[0]);
                ExpType right = evalExpType(node.child[1]);
                if ("<".equals(node.op) || "=".equals(node.op)) {
                    if (left != ExpType.Void && right != ExpType.Void && !typesCompatible(left, right)) {
                        errors.add("第" + node.lineno + "行：关系运算左右类型不兼容");
                    }
                    node.expType = ExpType.Boolean;
                    return ExpType.Boolean;
                }
                if (left != ExpType.Void && right != ExpType.Void && !typesCompatible(left, right)) {
                    errors.add("第" + node.lineno + "行：算术运算左右类型不兼容");
                }
                node.expType = ExpType.Integer;
                return ExpType.Integer;
            default:
                return ExpType.Void;
        }
    }

    private ExpType toExpType(String typeName) {
        if ("integer".equalsIgnoreCase(typeName) || "array".equalsIgnoreCase(typeName)) {
            return ExpType.Integer;
        }
        if ("char".equalsIgnoreCase(typeName)) {
            return ExpType.Char;
        }
        return ExpType.Integer;
    }

    private boolean typesCompatible(String typeName, ExpType expType) {
        ExpType declared = toExpType(typeName);
        return declared == expType || expType == ExpType.Integer;
    }

    private boolean typesCompatible(ExpType left, ExpType right) {
        return left == right || left == ExpType.Integer || right == ExpType.Integer;
    }
}
