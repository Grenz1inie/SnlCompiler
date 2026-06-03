package com.snl.compiler.service;

import com.snl.compiler.core.lexer.Lexer;
import com.snl.compiler.infra.config.Constants;
import com.snl.compiler.model.Token;

import java.util.ArrayList;
import java.util.List;

class TokenMapper {
    List<TokenDto> toDtos() {
        List<TokenDto> tokens = new ArrayList<TokenDto>();
        if (Constants.token == null) {
            return tokens;
        }
        for (Token token : Constants.token) {
            tokens.add(new TokenDto(token, resolveLexeme(token)));
        }
        return tokens;
    }

    private String resolveLexeme(Token token) {
        switch (token.i) {
            case 1:
                return Constants.separator.get(token.j);
            case 2:
                return Constants.reservedWord.get(token.j);
            case 3:
                return Lexer.identifier.get(token.j);
            case 4:
                return Lexer.INTC.get(token.j);
            case 5:
                return Lexer.CHARC.get(token.j);
            default:
                return "";
        }
    }
}
