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
import com.snl.compiler.infra.config.Constants;
import com.snl.compiler.model.Token;

public class RecursiveDescentParser {
    private int currentIndex = 0;
    private Token currentToken;
    private List<String> errors = new ArrayList<>();

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

    public List<String> getErrors() {
        return errors;
    }

    private void nextToken() {
        if (currentIndex < Constants.token.size()) {
            currentToken = Constants.token.get(currentIndex++);
        } else {
            currentToken = null;
        }
    }

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

    private boolean match(String expected) {
        if (currentToken != null && getLexeme(currentToken).equals(expected)) {
            nextToken();
            return true;
        }
        String found = currentToken != null ? getLexeme(currentToken) : "EOF";
        errors.add("Line " + (currentToken != null ? currentToken.l : "unknown") + ": Expected " + expected + " but found " + found);
        // Try to recover by skipping? No, let's just fail for now.
        return false;
    }

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

    private BaseASTNode declarePart() {
        BaseASTNode typeDec = typeDecPart();
        BaseASTNode varDec = varDecPart();
        BaseASTNode procDec = procDecPart();
        BaseASTNode decls = linkDecl(typeDec, varDec);
        return linkDecl(decls, procDec);
    }

    private BaseASTNode typeDecPart() {
        if (currentToken != null && getLexeme(currentToken).equals("type")) {
            nextToken();
            return typeDecList();
        }
        return null;
    }

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

    private BaseASTNode typeDecMore() {
        if (currentToken != null && getLexeme(currentToken).equals("ID")) {
            return typeDecList();
        }
        return null;
    }

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

    private BaseASTNode structureType() {
        String lexeme = getLexeme(currentToken);
        if (lexeme.equals("array")) return arrayType();
        else return recordType();
    }

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

    private BaseASTNode fieldDecMore() {
        if (currentToken != null && (getLexeme(currentToken).equals("integer")
                || getLexeme(currentToken).equals("char")
                || getLexeme(currentToken).equals("array"))) {
            return fieldDecList();
        }
        return null;
    }

    private BaseASTNode varDecPart() {
        if (currentToken != null && getLexeme(currentToken).equals("var")) {
            nextToken();
            return varDecList();
        }
        return null;
    }

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

    private BaseASTNode procDecPart() {
        if (currentToken != null && getLexeme(currentToken).equals("procedure")) {
            return procDec();
        }
        return null;
    }

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

    private BaseASTNode paramDecList() {
        BaseASTNode node = param();
        if (currentToken != null && getLexeme(currentToken).equals(";")) {
            nextToken();
            node.sibling = paramDecList();
        }
        return node;
    }

    private BaseASTNode param() {
        BaseASTNode node = new BaseASTNode();
        node.nodeKind = NodeKind.VarK;
        node.lineno = currentToken != null ? currentToken.l : 0;
        if (currentToken != null && getLexeme(currentToken).equals("var")) {
            nextToken();
            // Could add a flag for VAR params
        }
        node.child[0] = typeDef();
        node.name.addAll(idList());
        return node;
    }

    private BaseASTNode procDecMore() {
        if (currentToken != null && getLexeme(currentToken).equals("procedure")) {
            return procDec();
        }
        return null;
    }

    private BaseASTNode procBody() {
        return programBody();
    }

    private BaseASTNode programBody() {
        BaseASTNode node = new BaseASTNode();
        node.nodeKind = NodeKind.StmK;
        node.lineno = currentToken != null ? currentToken.l : 0;
        match("begin");
        node.child[0] = stmList();
        match("end");
        return node;
    }

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
