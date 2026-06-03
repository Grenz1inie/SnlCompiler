package com.snl.compiler.service;

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
    public List<String> errors = new ArrayList<String>();
    public List<TokenDto> tokens = new ArrayList<TokenDto>();

    public static CompileResponse of(String stage) {
        CompileResponse response = new CompileResponse();
        response.stage = stage;
        return response;
    }
}
