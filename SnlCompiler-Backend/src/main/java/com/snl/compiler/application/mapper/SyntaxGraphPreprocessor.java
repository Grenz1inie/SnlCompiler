package com.snl.compiler.application.mapper;

import com.snl.compiler.api.dto.SyntaxGraphDto;
import com.snl.compiler.api.dto.SyntaxGraphEdgeDto;
import com.snl.compiler.api.dto.SyntaxGraphNodeDto;
import com.snl.compiler.api.dto.SyntaxTreeNodeDto;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SyntaxGraphPreprocessor {
    private static final int NODE_WIDTH = 170;
    private static final int NODE_HEIGHT = 58;
    private static final int HORIZONTAL_GAP = 230;
    private static final int VERTICAL_GAP = 138;
    private static final int LEFT_PADDING = 80;
    private static final int TOP_PADDING = 36;

    public SyntaxGraphDto toGraph(List<SyntaxTreeNodeDto> treeNodes) {
        SyntaxGraphDto graph = new SyntaxGraphDto();
        if (treeNodes == null || treeNodes.isEmpty()) {
            return graph;
        }

        Map<Integer, Integer> depthCounts = new HashMap<Integer, Integer>();
        for (SyntaxTreeNodeDto node : treeNodes) {
            int count = depthCounts.containsKey(node.depth) ? depthCounts.get(node.depth) : 0;
            depthCounts.put(node.depth, count + 1);
        }

        Map<Integer, Integer> depthSeen = new HashMap<Integer, Integer>();
        for (SyntaxTreeNodeDto node : treeNodes) {
            int seen = depthSeen.containsKey(node.depth) ? depthSeen.get(node.depth) : 0;
            depthSeen.put(node.depth, seen + 1);

            int count = depthCounts.containsKey(node.depth) ? depthCounts.get(node.depth) : 1;
            int x = LEFT_PADDING + seen * HORIZONTAL_GAP;
            if (count == 1) {
                x = LEFT_PADDING + Math.max(0, maxDepthCount(depthCounts) - 1) * HORIZONTAL_GAP / 2;
            }
            int y = TOP_PADDING + node.depth * VERTICAL_GAP;
            String label = node.line > 0 ? node.label + "\nL" + node.line : node.label;

            graph.nodes.add(new SyntaxGraphNodeDto(
                    node.id,
                    "syntax-node",
                    x,
                    y,
                    NODE_WIDTH,
                    NODE_HEIGHT,
                    label,
                    node.kind,
                    node.line
            ));

            if (node.parentId != null && !node.parentId.isEmpty()) {
                graph.edges.add(new SyntaxGraphEdgeDto(
                        node.parentId + "-" + node.id,
                        node.parentId,
                        node.id,
                        "syntax-edge"
                ));
            }
        }
        return graph;
    }

    private int maxDepthCount(Map<Integer, Integer> depthCounts) {
        int max = 0;
        for (Integer count : depthCounts.values()) {
            if (count != null && count > max) {
                max = count;
            }
        }
        return max;
    }
}
