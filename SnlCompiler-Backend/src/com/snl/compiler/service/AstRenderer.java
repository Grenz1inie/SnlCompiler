package com.snl.compiler.service;

import com.snl.compiler.core.ast.BaseASTNode;

class AstRenderer {
    String render(BaseASTNode root) {
        if (root == null) {
            return "";
        }
        StringBuilder output = new StringBuilder();
        append(root, output, 0);
        return output.toString();
    }

    private void append(BaseASTNode node, StringBuilder output, int depth) {
        if (node == null) {
            return;
        }
        for (int i = 0; i < depth; i++) {
            output.append("  ");
        }
        output.append(node.nodeKind);
        if (node.decKind != null) {
            output.append("/").append(node.decKind);
        }
        if (node.stmKind != null) {
            output.append("/").append(node.stmKind);
        }
        if (node.expKind != null) {
            output.append("/").append(node.expKind);
        }
        if (!node.name.isEmpty()) {
            output.append(" ").append(node.name);
        }
        if (node.typeName != null) {
            output.append(" type=").append(node.typeName);
        }
        output.append(" line=").append(node.lineno).append("\n");

        for (int i = 0; i < node.child.length; i++) {
            append(node.child[i], output, depth + 1);
        }
        append(node.sibling, output, depth);
    }
}
