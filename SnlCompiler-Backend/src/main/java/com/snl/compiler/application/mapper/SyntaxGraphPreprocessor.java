package com.snl.compiler.application.mapper;

import com.snl.compiler.api.dto.SyntaxGraphDto;
import com.snl.compiler.api.dto.SyntaxGraphEdgeDto;
import com.snl.compiler.api.dto.SyntaxGraphNodeDto;
import com.snl.compiler.api.dto.SyntaxTreeNodeDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SyntaxGraphPreprocessor {
    private static final int NODE_WIDTH = 170;
    private static final int NODE_HEIGHT = 58;
    private static final int HORIZONTAL_GAP = 200;
    private static final int VERTICAL_GAP = 120;
    private static final int LEFT_PADDING = 60;
    private static final int TOP_PADDING = 40;

    private int nextLeafCenter;
    private final Map<String, Integer> centerXById = new HashMap<String, Integer>();
    private final Map<String, Integer> depthById = new HashMap<String, Integer>();

    public SyntaxGraphDto toGraph(List<SyntaxTreeNodeDto> treeNodes) {
        SyntaxGraphDto graph = new SyntaxGraphDto();
        if (treeNodes == null || treeNodes.isEmpty()) {
            return graph;
        }

        Map<String, List<SyntaxTreeNodeDto>> childrenByParent = new HashMap<String, List<SyntaxTreeNodeDto>>();
        String rootId = null;

        for (SyntaxTreeNodeDto node : treeNodes) {
            if (node.parentId == null || node.parentId.isEmpty()) {
                rootId = node.id;
            } else {
                List<SyntaxTreeNodeDto> siblings = childrenByParent.get(node.parentId);
                if (siblings == null) {
                    siblings = new ArrayList<SyntaxTreeNodeDto>();
                    childrenByParent.put(node.parentId, siblings);
                }
                siblings.add(node);
            }
        }

        for (List<SyntaxTreeNodeDto> children : childrenByParent.values()) {
            Collections.sort(children, new Comparator<SyntaxTreeNodeDto>() {
                @Override
                public int compare(SyntaxTreeNodeDto left, SyntaxTreeNodeDto right) {
                    return Integer.compare(left.order, right.order);
                }
            });
        }

        if (rootId == null) {
            rootId = treeNodes.get(0).id;
        }

        nextLeafCenter = 0;
        centerXById.clear();
        depthById.clear();
        assignLayout(rootId, childrenByParent, 0);
        normalizeHorizontalPositions();

        for (SyntaxTreeNodeDto node : treeNodes) {
            Integer centerX = centerXById.get(node.id);
            Integer depth = depthById.get(node.id);
            if (centerX == null || depth == null) {
                continue;
            }

            int x = LEFT_PADDING + centerX - NODE_WIDTH / 2;
            int y = TOP_PADDING + depth * VERTICAL_GAP;
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

    private int assignLayout(String nodeId, Map<String, List<SyntaxTreeNodeDto>> childrenByParent, int depth) {
        depthById.put(nodeId, depth);
        List<SyntaxTreeNodeDto> children = childrenByParent.get(nodeId);

        if (children == null || children.isEmpty()) {
            int center = nextLeafCenter;
            nextLeafCenter += HORIZONTAL_GAP;
            centerXById.put(nodeId, center);
            return center;
        }

        int firstCenter = -1;
        int lastCenter = -1;
        for (SyntaxTreeNodeDto child : children) {
            int childCenter = assignLayout(child.id, childrenByParent, depth + 1);
            if (firstCenter < 0) {
                firstCenter = childCenter;
            }
            lastCenter = childCenter;
        }

        int center = (firstCenter + lastCenter) / 2;
        centerXById.put(nodeId, center);
        return center;
    }

    private void normalizeHorizontalPositions() {
        int minLeft = Integer.MAX_VALUE;
        for (Integer center : centerXById.values()) {
            int left = LEFT_PADDING + center - NODE_WIDTH / 2;
            if (left < minLeft) {
                minLeft = left;
            }
        }
        if (minLeft < LEFT_PADDING) {
            int shift = LEFT_PADDING - minLeft;
            for (Map.Entry<String, Integer> entry : centerXById.entrySet()) {
                centerXById.put(entry.getKey(), entry.getValue() + shift);
            }
        }
    }
}
