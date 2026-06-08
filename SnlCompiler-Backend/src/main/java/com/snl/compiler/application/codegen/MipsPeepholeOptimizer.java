package com.snl.compiler.application.codegen;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MipsPeepholeOptimizer {
    String optimize(String mips) {
        if (mips == null || mips.length() == 0) {
            return mips;
        }
        String[] lines = mips.split("\n", -1);
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith(".") || trimmed.endsWith(":")) {
                out.add(line);
                continue;
            }
            if (i + 1 < lines.length) {
                String nextTrim = lines[i + 1].trim();
                if (trimmed.startsWith("j ") && nextTrim.endsWith(":")) {
                    String target = trimmed.substring(2).trim();
                    String label = nextTrim.substring(0, nextTrim.length() - 1);
                    if (target.equals(label)) {
                        continue;
                    }
                }
            }
            if (trimmed.contains(", $zero") && trimmed.startsWith("add ")) {
                String replaced = trimmed.replace(", $zero", "").replace("add ", "move ");
                out.add(line.replace(trimmed, replaced));
                continue;
            }
            out.add(line);
        }
        return joinLines(out);
    }

    private String joinLines(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }
}
