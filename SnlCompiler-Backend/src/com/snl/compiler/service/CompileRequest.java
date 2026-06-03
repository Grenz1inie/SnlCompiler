package com.snl.compiler.service;

public class CompileRequest {
    public String source;
    public String tokenView;

    public CompileRequest() {
    }

    public CompileRequest(String source, String tokenView) {
        this.source = source;
        this.tokenView = tokenView;
    }
}
