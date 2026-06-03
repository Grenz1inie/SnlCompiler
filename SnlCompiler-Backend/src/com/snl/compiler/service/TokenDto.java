package com.snl.compiler.service;

import com.snl.compiler.model.Token;

public class TokenDto {
    public int line;
    public int type;
    public int index;
    public String lexeme;

    public TokenDto() {
    }

    public TokenDto(Token token, String lexeme) {
        this.line = token.l;
        this.type = token.i;
        this.index = token.j;
        this.lexeme = lexeme;
    }
}
