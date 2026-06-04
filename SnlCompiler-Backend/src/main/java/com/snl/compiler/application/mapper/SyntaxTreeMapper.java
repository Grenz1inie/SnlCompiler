package com.snl.compiler.application.mapper;

import com.snl.compiler.api.dto.SyntaxTreeNodeDto;
import com.snl.compiler.core.ast.BaseASTNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SyntaxTreeMapper {
    public List<SyntaxTreeNodeDto> toDtos(BaseASTNode root) {
        List<SyntaxTreeNodeDto> nodes = new ArrayList<SyntaxTreeNodeDto>();
        append(root, null, 0, nodes);
        return nodes;
    }

    private void append(BaseASTNode node, String parentId, int depth, List<SyntaxTreeNodeDto> nodes) {
        if (node == null) {
            return;
        }

        String id = "n" + nodes.size();
        nodes.add(new SyntaxTreeNodeDto(
                id,
                parentId,
                label(node),
                kind(node),
                node.lineno,
                depth,
                nodes.size()
        ));

        for (int i = 0; i < node.child.length; i++) {
            append(node.child[i], id, depth + 1, nodes);
        }
        append(node.sibling, parentId, depth, nodes);
    }

    private String label(BaseASTNode node) {
        StringBuilder label = new StringBuilder();
        label.append(node.nodeKind);
        if (node.decKind != null) {
            label.append("/").append(node.decKind);
        }
        if (node.stmKind != null) {
            label.append("/").append(node.stmKind);
        }
        if (node.expKind != null) {
            label.append("/").append(node.expKind);
        }
        if (!node.name.isEmpty()) {
            label.append(" ").append(node.name);
        }
        if (node.typeName != null) {
            label.append(" : ").append(node.typeName);
        }
        return label.toString();
    }

    private String kind(BaseASTNode node) {
        if (node.nodeKind == null) {
            return "Unknown";
        }
        return node.nodeKind.toString();
    }
}
