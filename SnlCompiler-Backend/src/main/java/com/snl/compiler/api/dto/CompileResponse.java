package com.snl.compiler.api.dto;

import java.util.ArrayList;
import java.util.List;

public class CompileResponse {
    public String stage;
    public boolean success;
    public String output;
    public String externalTokenOutput;
    public String internalTokenOutput;
    public String grammarOutput;
    public String astOutput;
    public String symbolTableOutput;
    public String mipsOutput;
    public List<String> errors = new ArrayList<String>();
    public List<TokenDto> tokens = new ArrayList<TokenDto>();
    public List<SyntaxTreeNodeDto> syntaxTree = new ArrayList<SyntaxTreeNodeDto>();
    public SyntaxGraphDto syntaxGraph = new SyntaxGraphDto();

    public static CompileResponse of(String stage) {
        CompileResponse response = new CompileResponse();
        response.stage = stage;
        return response;
    }
}


