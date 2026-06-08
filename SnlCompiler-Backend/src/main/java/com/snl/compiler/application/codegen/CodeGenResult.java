package com.snl.compiler.application.codegen;

import java.util.ArrayList;
import java.util.List;

public class CodeGenResult {
    public String irOutput;
    public String irOptimizedOutput;
    public String mipsRawOutput;
    public String mipsOutput;
    public List<String> errors = new ArrayList<String>();
}
