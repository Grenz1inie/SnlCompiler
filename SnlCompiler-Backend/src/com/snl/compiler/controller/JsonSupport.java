package com.snl.compiler.controller;

import com.snl.compiler.service.CompileResponse;
import com.snl.compiler.service.TokenDto;

import java.util.List;

public final class JsonSupport {
    private JsonSupport() {
    }

    public static String extractString(String json, String key) {
        if (json == null || key == null) {
            return "";
        }
        String quotedKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(quotedKey);
        if (keyIndex < 0) {
            return "";
        }
        int colonIndex = json.indexOf(':', keyIndex + quotedKey.length());
        if (colonIndex < 0) {
            return "";
        }
        int start = json.indexOf('"', colonIndex + 1);
        if (start < 0) {
            return "";
        }
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                value.append(unescape(c));
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return value.toString();
            } else {
                value.append(c);
            }
        }
        return value.toString();
    }

    public static String toJson(CompileResponse response) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        field(json, "stage", response.stage).append(",");
        json.append("\"success\":").append(response.success).append(",");
        field(json, "output", response.output).append(",");
        field(json, "externalTokenOutput", response.externalTokenOutput).append(",");
        field(json, "internalTokenOutput", response.internalTokenOutput).append(",");
        field(json, "grammarOutput", response.grammarOutput).append(",");
        field(json, "astOutput", response.astOutput).append(",");
        field(json, "symbolTableOutput", response.symbolTableOutput).append(",");
        stringArray(json, "errors", response.errors).append(",");
        tokenArray(json, response.tokens);
        json.append("}");
        return json.toString();
    }

    private static StringBuilder field(StringBuilder json, String name, String value) {
        json.append("\"").append(name).append("\":");
        if (value == null) {
            json.append("null");
        } else {
            json.append("\"").append(escape(value)).append("\"");
        }
        return json;
    }

    private static StringBuilder stringArray(StringBuilder json, String name, List<String> values) {
        json.append("\"").append(name).append("\":[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"").append(escape(values.get(i))).append("\"");
        }
        json.append("]");
        return json;
    }

    private static void tokenArray(StringBuilder json, List<TokenDto> tokens) {
        json.append("\"tokens\":[");
        for (int i = 0; i < tokens.size(); i++) {
            TokenDto token = tokens.get(i);
            if (i > 0) {
                json.append(",");
            }
            json.append("{");
            json.append("\"line\":").append(token.line).append(",");
            json.append("\"type\":").append(token.type).append(",");
            json.append("\"index\":").append(token.index).append(",");
            field(json, "lexeme", token.lexeme);
            json.append("}");
        }
        json.append("]");
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    escaped.append(c);
                    break;
            }
        }
        return escaped.toString();
    }

    private static char unescape(char c) {
        switch (c) {
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            case '"':
                return '"';
            case '\\':
                return '\\';
            default:
                return c;
        }
    }
}
