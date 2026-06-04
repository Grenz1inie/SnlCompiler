package com.snl.compiler.application.mapper;

import com.snl.compiler.api.dto.TokenDto;
import com.snl.compiler.core.lexer.Lexer;
import com.snl.compiler.domain.token.Token;
import com.snl.compiler.infrastructure.config.Constants;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TokenMapper {
    public List<TokenDto> toDtos() {
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
