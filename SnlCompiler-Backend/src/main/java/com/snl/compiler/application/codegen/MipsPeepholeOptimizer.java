package com.snl.compiler.application.codegen;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MIPS 窥孔优化：在已生成的汇编上做局部模式替换。
 * 当前规则：删除跳到下一条指令的冗余 j；add ..., $zero → move。
 */
@Component
public class MipsPeepholeOptimizer {

    /** 逐行扫描 MIPS 文本并应用窥孔规则 */
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
