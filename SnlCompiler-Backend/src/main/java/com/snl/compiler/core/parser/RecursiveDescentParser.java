package com.snl.compiler.core.parser;

import java.util.ArrayList;
import java.util.List;

import com.snl.compiler.core.ast.BaseASTNode;
import com.snl.compiler.core.ast.BaseASTNode.DecKind;
import com.snl.compiler.core.ast.BaseASTNode.ExpKind;
import com.snl.compiler.core.ast.BaseASTNode.ExpType;
import com.snl.compiler.core.ast.BaseASTNode.NodeKind;
import com.snl.compiler.core.ast.BaseASTNode.StmKind;
import com.snl.compiler.core.ast.BaseASTNode.VarKind;
import com.snl.compiler.core.lexer.Lexer;
import com.snl.compiler.infrastructure.config.Constants;
import com.snl.compiler.domain.token.Token;

/**
 * 递归下降语法分析器：每个非终结符对应一个方法，边读 Token 边构建 AST。
 * <p>
 * 与 LL(1) 分析器 {@link com.snl.compiler.core.parser.Parser} 共用
 * {@link Constants#token}，但本类输出 {@link BaseASTNode} 语法树供语义分析使用。
 * <p>
 * 文法结构对应关系示例：
 * program → programHead + declarePart + programBody + '.'
 * exp → term {( '+' | '-' ) term}
 * <p>
 * 解析顺序：
 * 1. parse() 重置状态，读取第一个 Token，进入 program()。
 * 2. program() 解析程序头、声明部、语句体和结束符 '.'。
 * 3. declarePart() 解析类型声明、变量声明、过程声明。
 * 4. programBody() / stmList() / stm() 解析复合语句和普通语句。
 * 5. relExp() / exp() / term() / factor() / variable() 解析表达式与变量。
 */
public class RecursiveDescentParser {

    // ==================== 1. 入口与公共工具 ====================

    /** 当前读到第几个 Token（下标） */
    private int currentIndex = 0;
    /** 当前向前看 Token */
    private Token currentToken;
    /** 语法错误信息列表 */
    private List<String> errors = new ArrayList<>();

    /**
     * 步骤 1：分析入口。
     * - 清空历史状态
     * - 检查 token 是否为空
     * - 预读第一个 token
     * - 从 program() 开始构建 AST 根节点
     */
    public BaseASTNode parse() {
        currentIndex = 0;
        errors.clear();
        if (Constants.token == null || Constants.token.isEmpty()) {
            errors.add("No tokens to parse.");
            return null;
        }
        nextToken();
        return program();
    }

    /** 返回本次语法分析收集到的错误信息。 */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * 步骤 2.1：向前读一个 Token。
     * currentIndex 始终指向“下一个待读取”的位置。
     */
    private void nextToken() {
        if (currentIndex < Constants.token.size()) {
            currentToken = Constants.token.get(currentIndex++);
        } else {
            currentToken = null;
        }
    }

    /**
     * 步骤 2.2：将 Token 还原为语法层终结符名称。
     * - 标识符/常量统一为 ID、INTC、CHARC
     * - 分隔符和保留字按词法表下标还原
     * 这样可以直接和语法层的终结符命名对齐。
     */
    private String getLexeme(Token t) {
        if (t == null) return "";
        switch (t.i) {
            case 1: return Constants.separator.get(t.j);
            case 2: return Constants.reservedWord.get(t.j);
            case 3: return "ID";
            case 4: return "INTC";
            case 5: return "CHARC";
            default: return "";
        }
    }

    /**
     * 步骤 2.3：获取 Token 的真实词素文本。
     * 用于 AST 节点、符号表和错误提示中保留原始名字，而不是语法层归一化名称。
     */
    private String getRealLexeme(Token t) {
        if (t == null) return "";
        switch (t.i) {
            case 1: return Constants.separator.get(t.j);
            case 2: return Constants.reservedWord.get(t.j);
            case 3: return Lexer.identifier.get(t.j);
            case 4: return Lexer.INTC.get(t.j);
            case 5: return Lexer.CHARC.get(t.j);
            default: return "";
        }
    }

    /**
     * 步骤 2.4：匹配并消费当前 token。
     * 匹配成功则前进一格；失败则记录带行号的错误信息。
     */
    private boolean match(String expected) {
        if (currentToken != null && getLexeme(currentToken).equals(expected)) {
            nextToken();
            return true;
        }
        String found = currentToken != null ? getLexeme(currentToken) : "EOF";
        errors.add("Line " + (currentToken != null ? currentToken.l : "unknown") + ": Expected " + expected + " but found " + found);
        return false;
    }

    // ==================== 2. 程序结构 ====================

    /**
     * 步骤 3：Program -> programHead declarePart programBody '.'
     * 这是整个 SNL 程序的根非终结符。
     */
    private BaseASTNode program() {
        BaseASTNode node = new BaseASTNode();
        node.nodeKind = NodeKind.ProK;
        node.lineno = currentToken != null ? currentToken.l : 0;
        node.child[0] = programHead();
        node.child[1] = declarePart();
        node.child[2] = programBody();
        if (currentToken != null && getLexeme(currentToken).equals(".")) {
            match(".");
        }
        return node;
    }

    /**
     * 步骤 3.1：ProgramHead -> program ID
     * 记录程序名到 AST 的 ProK 节点。
     */
    private BaseASTNode programHead() {
        BaseASTNode node = new BaseASTNode();
        node.nodeKind = NodeKind.PheadK;
        node.lineno = currentToken != null ? currentToken.l : 0;
        match("program");
        if (currentToken != null && getLexeme(currentToken).equals("ID")) {
            node.name.add(getRealLexeme(currentToken));
            nextToken();
        }
        return node;
    }

    /**
     * 工具方法：把一个声明节点接到 sibling 链表尾部。
     * 用于把 type / var / proc 声明串成一个声明序列。
     */
    private BaseASTNode linkDecl(BaseASTNode head, BaseASTNode node) {
        if (node == null) {
            return head;
        }
        if (head == null) {
            return node;
        }
        BaseASTNode tail = head;
        while (tail.sibling != null) {
            tail = tail.sibling;
        }
        tail.sibling = node;
        return head;
    }

    // ==================== 3. 声明部分（类型 / 变量 / 过程） ====================

    /**
     * 步骤 4：DeclarePart -> TypeDec VarDec ProcDec
     * 注意：三类声明在这里按顺序尝试解析，缺省分支返回 null。
     */
    private BaseASTNode declarePart() {
        BaseASTNode typeDec = typeDecPart();
        BaseASTNode varDec = varDecPart();
        BaseASTNode procDec = procDecPart();
        BaseASTNode decls = linkDecl(typeDec, varDec);
        return linkDecl(decls, procDec);
    }

    /** 解析可选的 type 声明部分。 */
    private BaseASTNode typeDecPart() {
        if (currentToken != null && getLexeme(currentToken).equals("type")) {
            nextToken();
            return typeDecList();
        }
        return null;
    }

    /** 解析一个或多个类型定义：ID = TypeDef ; ... */
    private BaseASTNode typeDecList() {
        BaseASTNode node = new BaseASTNode();
        node.nodeKind = NodeKind.TypeK;
        node.lineno = currentToken != null ? currentToken.l : 0;
        
        if (currentToken != null && getLexeme(currentToken).equals("ID")) {
            node.name.add(getRealLexeme(currentToken));
            nextToken();
            match("=");
            node.child[0] = typeDef();
            match(";");
            node.sibling = typeDecMore();
        }
        return node;
    }

    /** 继续解析后续类型定义。 */
    private BaseASTNode typeDecMore() {
        if (currentToken != null && getLexeme(currentToken).equals("ID")) {
            return typeDecList();
        }
        return null;
    }

    /** 解析类型定义右部：基本类型、数组类型、记录类型或别名类型。 */
    private BaseASTNode typeDef() {
        BaseASTNode node = new BaseASTNode();
        node.lineno = currentToken != null ? currentToken.l : 0;
        String lexeme = getLexeme(currentToken);
        if (lexeme.equals("integer") || lexeme.equals("char")) {
            node.nodeKind = NodeKind.TypeK;
            node.decKind = lexeme.equals("integer") ? DecKind.IntegerK : DecKind.CharK;
            nextToken();
        } else if (lexeme.equals("array") || lexeme.equals("record")) {
            node = structureType();
        } else if (lexeme.equals("ID")) {
            node.nodeKind = NodeKind.TypeK;
            node.decKind = DecKind.IdK;
            node.typeName = getRealLexeme(currentToken);
            nextToken();
        }
        return node;
    }

    /** 类型定义分支：array 或 record。 */
    private BaseASTNode structureType() {
        String lexeme = getLexeme(currentToken);
        if (lexeme.equals("array")) return arrayType();
        else return recordType();
    }

    /** 解析数组类型：array [ low .. high ] of baseType */
    private BaseASTNode arrayType() {
        BaseASTNode node = new BaseASTNode();
        node.nodeKind = NodeKind.TypeK;
        node.decKind = DecKind.ArrayK;
        node.lineno = currentToken != null ? currentToken.l : 0;
        match("array");
        match("[");
        if (currentToken != null && getLexeme(currentToken).equals("INTC")) {
            node.arrayLow = Integer.parseInt(getRealLexeme(currentToken));
            nextToken();
        }
        match("..");
        if (currentToken != null && getLexeme(currentToken).equals("INTC")) {
            node.arrayHigh = Integer.parseInt(getRealLexeme(currentToken));
            nextToken();
        }
        match("]");
        match("of");
        node.child[0] = baseType();
        return node;
    }

    /** 解析基础类型：integer / char。 */
    private BaseASTNode baseType() {
        BaseASTNode node = new BaseASTNode();
        node.nodeKind = NodeKind.TypeK;
        node.lineno = currentToken != null ? currentToken.l : 0;
        String lexeme = getLexeme(currentToken);
        if (lexeme.equals("integer")) {
            node.decKind = DecKind.IntegerK;
            nextToken();
        } else if (lexeme.equals("char")) {
            node.decKind = DecKind.CharK;
            nextToken();
        }
        return node;
    }

    /** 解析记录类型：record fieldDecList end。 */
    private BaseASTNode recordType() {
        BaseASTNode node = new BaseASTNode();
        node.nodeKind = NodeKind.TypeK;
        node.decKind = DecKind.RecordK;
        node.lineno = currentToken != null ? currentToken.l : 0;
        match("record");
        node.child[0] = fieldDecList();
        match("end");
        return node;
    }

    /** 解析记录字段声明：基础类型/数组类型 + 标识符列表 + ';'。 */
    private BaseASTNode fieldDecList() {
        if (currentToken == null) {
            return null;
        }
        String lexeme = getLexeme(currentToken);
        if (!lexeme.equals("integer") && !lexeme.equals("char") && !lexeme.equals("array")) {
            return null;
        }
        BaseASTNode node = new BaseASTNode();
        node.nodeKind = NodeKind.TypeK;
        node.lineno = currentToken != null ? currentToken.l : 0;
        if (lexeme.equals("array")) {
            node.child[0] = arrayType();
        } else {
            node.child[0] = baseType();
        }
        node.name.addAll(idList());
        match(";");
        node.sibling = fieldDecMore();
        return node;
    }

    /** 继续解析后续字段声明。 */
    private BaseASTNode fieldDecMore() {
        if (currentToken != null && (getLexeme(currentToken).equals("integer")
                || getLexeme(currentToken).equals("char")
                || getLexeme(currentToken).equals("array"))) {
            return fieldDecList();
        }
        return null;
    }

    /** 解析可选的变量声明部分。 */
    private BaseASTNode varDecPart() {
        if (currentToken != null && getLexeme(currentToken).equals("var")) {
            nextToken();
            return varDecList();
        }
        return null;
    }

    /** 解析变量声明：TypeDef idList ; ... */
    private BaseASTNode varDecList() {
        BaseASTNode node = new BaseASTNode();
        node.nodeKind = NodeKind.VarK;
        node.lineno = currentToken != null ? currentToken.l : 0;
        node.child[0] = typeDef();
        node.name.addAll(idList());
        match(";");
        node.sibling = varDecMore();
        return node;
    }

    /** 解析逗号分隔的标识符列表。 */
    private List<String> idList() {
        List<String> ids = new ArrayList<>();
        if (currentToken != null && getLexeme(currentToken).equals("ID")) {
            ids.add(getRealLexeme(currentToken));
            nextToken();
            while (currentToken != null && getLexeme(currentToken).equals(",")) {
                nextToken();
                if (currentToken != null && getLexeme(currentToken).equals("ID")) {
                    ids.add(getRealLexeme(currentToken));
                    nextToken();
                }
            }
        }
        return ids;
    }

    /** 继续解析后续变量声明。 */
    private BaseASTNode varDecMore() {
        if (currentToken != null && (getLexeme(currentToken).equals("integer") || 
            getLexeme(currentToken).equals("char") || 
            getLexeme(currentToken).equals("array") || 
            getLexeme(currentToken).equals("record") || 
            getLexeme(currentToken).equals("ID"))) {
            return varDecList();
        }
        return null;
    }

    /** 解析可选的过程声明部分。 */
    private BaseASTNode procDecPart() {
        if (currentToken != null && getLexeme(currentToken).equals("procedure")) {
            return procDec();
        }
        return null;
    }

    /** 解析单个过程定义。 */
    private BaseASTNode procDec() {
        BaseASTNode node = new BaseASTNode();
        node.nodeKind = NodeKind.ProcDecK;
        node.lineno = currentToken != null ? currentToken.l : 0;
        match("procedure");
        if (currentToken != null && getLexeme(currentToken).equals("ID")) {
            node.name.add(getRealLexeme(currentToken));
            nextToken();
        }
        match("(");
        node.child[0] = paramList();
        match(")");
        match(";");
        node.child[1] = declarePart();
        node.child[2] = procBody();
        node.sibling = procDecMore();
        return node;
    }

    /** 解析过程参数列表，允许为空。 */
    private BaseASTNode paramList() {
        if (currentToken != null && (getLexeme(currentToken).equals("var") || 
            getLexeme(currentToken).equals("integer") || 
            getLexeme(currentToken).equals("char") || 
            getLexeme(currentToken).equals("array") || 
            getLexeme(currentToken).equals("record") || 
            getLexeme(currentToken).equals("ID"))) {
            return paramDecList();
        }
        return null;
    }

    /** 解析多个参数声明，参数之间用 ';' 分隔。 */
    private BaseASTNode paramDecList() {
        BaseASTNode node = param();
        if (currentToken != null && getLexeme(currentToken).equals(";")) {
            nextToken();
            node.sibling = paramDecList();
        }
        return node;
    }

    /** 解析一个参数：可选 var + TypeDef + idList。 */
    private BaseASTNode param() {
        BaseASTNode node = new BaseASTNode();
        node.nodeKind = NodeKind.VarK;
        node.lineno = currentToken != null ? currentToken.l : 0;
        if (currentToken != null && getLexeme(currentToken).equals("var")) {
            nextToken();
            // 这里可扩展成“引用传递参数”标记；当前 AST 仅记录语法结构。
        }
        node.child[0] = typeDef();
        node.name.addAll(idList());
        return node;
    }

    /** 继续解析后续过程声明。 */
    private BaseASTNode procDecMore() {
        if (currentToken != null && getLexeme(currentToken).equals("procedure")) {
            return procDec();
        }
        return null;
    }

    /** 过程体与普通语句体共用同一个 begin...end 结构。 */
    private BaseASTNode procBody() {
        return programBody();
    }

    // ==================== 4. 语句部分 ====================

    /**
     * 步骤 5：ProgramBody -> begin StmList end
     * 过程体也复用这个入口。
     */
    private BaseASTNode programBody() {
        BaseASTNode node = new BaseASTNode();
        node.nodeKind = NodeKind.StmK;
        node.lineno = currentToken != null ? currentToken.l : 0;
        match("begin");
        node.child[0] = stmList();
        match("end");
        return node;
    }

    /** 解析语句序列；遇到结束关键字时返回 null。 */
    private BaseASTNode stmList() {
        if (currentToken == null || getLexeme(currentToken).equals("end")
                || getLexeme(currentToken).equals("else")
                || getLexeme(currentToken).equals("fi")
                || getLexeme(currentToken).equals("endwh")) {
            return null;
        }
        BaseASTNode node = stm();
        if (node == null || node.stmKind == null) {
            return null;
        }
        if (currentToken != null && getLexeme(currentToken).equals(";")) {
            nextToken();
            node.sibling = stmList();
        }
        return node;
    }

    /**
     * 步骤 5.1：解析单条语句。
     * 支持 if / while / read / write / return / 赋值 / 过程调用。
     */
    private BaseASTNode stm() {
        BaseASTNode node = new BaseASTNode();
        node.lineno = currentToken != null ? currentToken.l : 0;
        String lexeme = getLexeme(currentToken);
        if (lexeme.equals("if")) {
            node.nodeKind = NodeKind.StmK;
            node.stmKind = StmKind.IfK;
            nextToken();
            node.child[0] = relExp();
            match("then");
            node.child[1] = stmList();
            match("else");
            node.child[2] = stmList();
            match("fi");
        } else if (lexeme.equals("while")) {
            node.nodeKind = NodeKind.StmK;
            node.stmKind = StmKind.WhileK;
            nextToken();
            node.child[0] = relExp();
            match("do");
            node.child[1] = stmList();
            match("endwh");
        } else if (lexeme.equals("read")) {
            node.nodeKind = NodeKind.StmK;
            node.stmKind = StmKind.ReadK;
            nextToken();
            match("(");
            if (currentToken != null && getLexeme(currentToken).equals("ID")) {
                node.name.add(getRealLexeme(currentToken));
                nextToken();
            }
            match(")");
        } else if (lexeme.equals("write")) {
            node.nodeKind = NodeKind.StmK;
            node.stmKind = StmKind.WriteK;
            nextToken();
            match("(");
            node.child[0] = exp();
            match(")");
        } else if (lexeme.equals("return")) {
            node.nodeKind = NodeKind.StmK;
            node.stmKind = StmKind.ReturnK;
            nextToken();
            match("(");
            node.child[0] = exp();
            match(")");
        } else if (lexeme.equals("ID")) {
            BaseASTNode varNode = variable();
            if (currentToken != null && getLexeme(currentToken).equals(":=")) {
                node.nodeKind = NodeKind.StmK;
                node.stmKind = StmKind.AssignK;
                node.child[0] = varNode;
                nextToken();
                node.child[1] = exp();
            } else if (currentToken != null && getLexeme(currentToken).equals("(")) {
                node.nodeKind = NodeKind.StmK;
                node.stmKind = StmKind.CallK;
                if (varNode != null && !varNode.name.isEmpty()) {
                    node.name.add(varNode.name.get(0));
                }
                nextToken();
                node.child[0] = actParamList();
                match(")");
            } else {
                return null;
            }
        } else {
            return null;
        }
        return node;
    }

    // ==================== 5. 表达式部分（优先级：relExp > exp > term > factor） ====================

    /** 步骤 6.1：关系表达式。 */
    private BaseASTNode relExp() {
        BaseASTNode node = exp();
        if (currentToken != null && (getLexeme(currentToken).equals("<") || getLexeme(currentToken).equals("="))) {
            BaseASTNode opNode = new BaseASTNode();
            opNode.nodeKind = NodeKind.ExpK;
            opNode.expKind = ExpKind.OpK;
            opNode.op = getLexeme(currentToken);
            opNode.lineno = currentToken.l;
            nextToken();
            opNode.child[0] = node;
            opNode.child[1] = exp();
            return opNode;
        }
        return node;
    }

    /** 步骤 6.2：加减表达式。 */
    private BaseASTNode exp() {
        BaseASTNode node = term();
        while (currentToken != null && (getLexeme(currentToken).equals("+") || getLexeme(currentToken).equals("-"))) {
            BaseASTNode opNode = new BaseASTNode();
            opNode.nodeKind = NodeKind.ExpK;
            opNode.expKind = ExpKind.OpK;
            opNode.op = getLexeme(currentToken);
            opNode.lineno = currentToken.l;
            nextToken();
            opNode.child[0] = node;
            opNode.child[1] = term();
            node = opNode;
        }
        return node;
    }

    /** 步骤 6.3：乘除表达式。 */
    private BaseASTNode term() {
        BaseASTNode node = factor();
        while (currentToken != null && (getLexeme(currentToken).equals("*") || getLexeme(currentToken).equals("/"))) {
            BaseASTNode opNode = new BaseASTNode();
            opNode.nodeKind = NodeKind.ExpK;
            opNode.expKind = ExpKind.OpK;
            opNode.op = getLexeme(currentToken);
            opNode.lineno = currentToken.l;
            nextToken();
            opNode.child[0] = node;
            opNode.child[1] = factor();
            node = opNode;
        }
        return node;
    }

    /** 解析变量：ID 后面可能继续跟数组下标或记录字段访问。 */
    private BaseASTNode variable() {
        BaseASTNode node = new BaseASTNode();
        node.nodeKind = NodeKind.ExpK;
        node.expKind = ExpKind.IdK;
        node.varKind = VarKind.IdV;
        node.lineno = currentToken != null ? currentToken.l : 0;
        if (currentToken != null && getLexeme(currentToken).equals("ID")) {
            node.name.add(getRealLexeme(currentToken));
            nextToken();
        }
        return parseVariMore(node);
    }

    /** 解析变量后缀：[...] 或 .field，支持链式访问。 */
    private BaseASTNode parseVariMore(BaseASTNode base) {
        BaseASTNode current = base;
        while (currentToken != null) {
            if (getLexeme(currentToken).equals("[")) {
                nextToken();
                BaseASTNode index = exp();
                match("]");
                BaseASTNode arrNode = new BaseASTNode();
                arrNode.nodeKind = NodeKind.ExpK;
                arrNode.expKind = ExpKind.IdK;
                arrNode.varKind = VarKind.ArrayV;
                arrNode.lineno = current.lineno;
                arrNode.child[0] = current;
                arrNode.child[1] = index;
                current = arrNode;
            } else if (getLexeme(currentToken).equals(".")) {
                nextToken();
                BaseASTNode fieldNode = new BaseASTNode();
                fieldNode.nodeKind = NodeKind.ExpK;
                fieldNode.expKind = ExpKind.IdK;
                fieldNode.varKind = VarKind.FieldV;
                fieldNode.lineno = currentToken != null ? currentToken.l : current.lineno;
                fieldNode.child[0] = current;
                if (currentToken != null && getLexeme(currentToken).equals("ID")) {
                    fieldNode.name.add(getRealLexeme(currentToken));
                    nextToken();
                } else {
                    errors.add("Line " + fieldNode.lineno + ": Expected field name after '.'");
                }
                current = fieldNode;
            } else {
                break;
            }
        }
        return current;
    }

    /** 步骤 6.4：因子。括号、常量、变量都在这里归一。 */
    private BaseASTNode factor() {
        BaseASTNode node = new BaseASTNode();
        node.lineno = currentToken != null ? currentToken.l : 0;
        String lexeme = getLexeme(currentToken);
        if (lexeme.equals("(")) {
            nextToken();
            node = exp();
            match(")");
        } else if (lexeme.equals("INTC")) {
            node.nodeKind = NodeKind.ExpK;
            node.expKind = ExpKind.ConstK;
            node.expType = ExpType.Integer;
            node.val = Integer.parseInt(getRealLexeme(currentToken));
            nextToken();
        } else if (lexeme.equals("CHARC")) {
            node.nodeKind = NodeKind.ExpK;
            node.expKind = ExpKind.ConstK;
            node.expType = ExpType.Char;
            node.name.add(getRealLexeme(currentToken));
            nextToken();
        } else if (lexeme.equals("ID")) {
            node = variable();
        }
        return node;
    }

    /** 解析实参列表；空参数列表返回 null。 */
    private BaseASTNode actParamList() {
        if (currentToken != null && !getLexeme(currentToken).equals(")")) {
            BaseASTNode node = exp();
            if (currentToken != null && getLexeme(currentToken).equals(",")) {
                nextToken();
                node.sibling = actParamList();
            }
            return node;
        }
        return null;
    }
}
