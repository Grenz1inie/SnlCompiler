package com.snl.compiler.api.dto;

public class SyntaxGraphEdgeDto {
    public String id;
    public String source;
    public String target;
    public String shape;

    public SyntaxGraphEdgeDto() {
    }

    public SyntaxGraphEdgeDto(String id, String source, String target, String shape) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.shape = shape;
    }
}


