package com.snl.compiler.api.dto;

public class SyntaxTreeNodeDto {
    public String id;
    public String parentId;
    public String label;
    public String kind;
    public int line;
    public int depth;
    public int order;

    public SyntaxTreeNodeDto() {
    }

    public SyntaxTreeNodeDto(String id, String parentId, String label, String kind, int line, int depth, int order) {
        this.id = id;
        this.parentId = parentId;
        this.label = label;
        this.kind = kind;
        this.line = line;
        this.depth = depth;
        this.order = order;
    }
}


