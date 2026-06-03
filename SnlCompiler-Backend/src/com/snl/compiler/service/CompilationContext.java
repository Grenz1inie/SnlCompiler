package com.snl.compiler.service;

import com.snl.compiler.core.ast.BaseASTNode;

import java.util.ArrayList;
import java.util.List;

class CompilationContext {
    boolean lexicalSuccess;
    BaseASTNode astRoot;
    final List<String> parseErrors = new ArrayList<String>();
}
