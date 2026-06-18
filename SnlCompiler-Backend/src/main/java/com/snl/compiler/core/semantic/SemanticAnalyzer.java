package com.snl.compiler.core.semantic;

import java.util.ArrayList;
import java.util.List;

import com.snl.compiler.core.ast.BaseASTNode;
import com.snl.compiler.core.ast.BaseASTNode.DecKind;
import com.snl.compiler.core.ast.BaseASTNode.ExpType;
import com.snl.compiler.core.ast.BaseASTNode.VarKind;
import com.snl.compiler.domain.symbol.Symbol;
import com.snl.compiler.domain.symbol.Symbol.SymbolKind;
import com.snl.compiler.domain.symbol.SymbolTable;

/**
 * 语义分析器：遍历递归下降生成的 AST，建立符号表并检查语义错误。
 * <p>
 * 主要检查：重复定义、未声明标识符、赋值/运算类型兼容、条件表达式类型、过程调用等。
 * 分析通过后，符号表与 AST 供 {@link com.snl.compiler.application.codegen.IrGenerator} 生成中间代码。
 * <p>
 * 解析/分析顺序：
 * 1. analyze() 从 AST 根节点开始。
 * 2. traverse() 按节点种类分发到声明、语句、表达式处理函数。
 * 3. 声明阶段把类型、变量、过程写入符号表，并检查重复定义。
 * 4. 语句阶段检查赋值、if/while、read/write/call 的合法性。
 * 5. 表达式阶段推导类型，检查算术和关系运算的类型兼容。
 */
public class SemanticAnalyzer {

    // ==================== 1. 状态 ====================

    /** 全局符号表（含作用域栈） */
    private SymbolTable symbolTable;
    /** 语义错误信息列表 */
    private List<String> errors;

    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
        this.errors = new ArrayList<>();
    }

    /**
     * 步骤 1：语义分析入口。
     * 1. 先检查根节点是否为空。
     * 2. 再从根节点开始深度优先遍历。
     */
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

    /**
     * 步骤 2：按节点类型分发处理。
     * 1. ProK 负责继续遍历子树。
     * 2. TypeK/VarK/ProcDecK 负责建符号和开闭作用域。
     * 3. StmK 负责语句语义检查。
     * 4. ExpK 负责表达式类型推导。
     */
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

    /**
     * 工具步骤：把 AST 中的类型节点统一归一成字符串类型名。
     * 这里既处理基础类型，也处理数组、记录和类型别名。
     */
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

    /**
     * 步骤 3.1：处理类型声明。
     * 1. 为每个类型名生成符号。
     * 2. 记录实际类型信息。
     * 3. 如果是数组，额外记录上下界和基类型。
     * 4. 插入符号表并检查重复定义。
     */
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

    /**
     * 步骤 3.2：处理变量声明。
     * 1. 先解析声明的类型。
     * 2. 再把每个变量名写入符号表。
     * 3. 若变量是数组，也记录数组上下界和基类型。
     */
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

    /**
     * 步骤 3.3：处理过程声明。
     * 1. 过程名先进入当前作用域。
     * 2. 进入过程内层作用域。
     * 3. 依次处理参数、局部声明和过程体。
     * 4. 退出过程作用域。
     */
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

    /**
     * 步骤 4：处理语句。
     * 1. 赋值语句检查左值是否可赋值，再比对左右类型。
     * 2. if/while 检查条件表达式类型。
     * 3. call 检查过程是否已声明且确实是过程名。
     * 4. read/write/return 检查参数或子表达式合法性。
     */
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

    /**
     * 工具步骤：检查一个变量节点是否可作为左值使用。
     * 会递归穿过数组下标和记录字段，最终确认基标识符是否已声明且不是类型名/过程名。
     */
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

    /**
     * 工具步骤：检查 read 之类语句中出现的普通变量标识符是否合法。
     */
    private void checkVarIdentifier(String name, int line) {
        Symbol sym = symbolTable.lookup(name);
        if (sym == null) {
            errors.add("第" + line + "行：变量 '" + name + "' 未声明");
        } else if (sym.kind != SymbolKind.VarK) {
            errors.add("第" + line + "行：'" + name + "' 不是变量标识符");
        }
    }

    /** 表达式节点统一走类型推导入口。 */
    private void handleExp(BaseASTNode node) {
        evalExpType(node);
    }

    /**
     * 步骤 5：推导表达式类型。
     * 1. 常量直接归类为 integer 或 char。
     * 2. 标识符需要查符号表并确认不是类型名/过程名。
     * 3. 运算节点递归推导左右子树，再判断算术或关系运算类型是否兼容。
     */
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

    /** 工具步骤：把符号表里的类型名映射成表达式类型。 */
    private ExpType toExpType(String typeName) {
        if ("integer".equalsIgnoreCase(typeName) || "array".equalsIgnoreCase(typeName)) {
            return ExpType.Integer;
        }
        if ("char".equalsIgnoreCase(typeName)) {
            return ExpType.Char;
        }
        return ExpType.Integer;
    }

    /** 工具步骤：判断声明类型和表达式类型是否兼容。 */
    private boolean typesCompatible(String typeName, ExpType expType) {
        ExpType declared = toExpType(typeName);
        return declared == expType || expType == ExpType.Integer;
    }

    /** 工具步骤：判断左右子表达式的类型是否兼容。 */
    private boolean typesCompatible(ExpType left, ExpType right) {
        return left == right || left == ExpType.Integer || right == ExpType.Integer;
    }
}
