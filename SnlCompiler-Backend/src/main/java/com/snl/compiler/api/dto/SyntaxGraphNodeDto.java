package com.snl.compiler.api.dto;

public class SyntaxGraphNodeDto {
    public String id;
    public String shape;
    public int x;
    public int y;
    public int width;
    public int height;
    public String label;
    public String kind;
    public int line;

    public SyntaxGraphNodeDto() {
    }

    public SyntaxGraphNodeDto(String id, String shape, int x, int y, int width, int height, String label, String kind, int line) {
        this.id = id;
        this.shape = shape;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.kind = kind;
        this.line = line;
    }
}


