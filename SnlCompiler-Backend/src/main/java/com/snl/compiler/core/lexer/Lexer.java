package com.snl.compiler.core.lexer;

import java.util.ArrayList;
import java.util.List;

import com.snl.compiler.infrastructure.config.Constants;
import com.snl.compiler.domain.token.Token;

public class Lexer {
	public static List<String> identifier;
	public static List<String> INTC;
	public static List<String> CHARC;

	public static boolean isIdentifier(String s) {
		if (!Constants.L.contains(s.charAt(0))) {
			return false;
		}
		for (int i = 1; i < s.length(); i++) {
			if (!Constants.L.contains(s.charAt(i)) && !Constants.D.contains(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean isINTC(String s) {
		if (s.charAt(0) == '0') {
			return s.length() == 1;
		}
		if (!Constants.D1.contains(s.charAt(0))) {
			return false;
		}
		for (int i = 1; i < s.length(); i++) {
			if (!Constants.D.contains(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static void fail(int line, String msg) {
		Constants.tokenShow.append("\n词法分析失败：第" + line + "行发生词法错误：" + msg);
		Constants.tokenShow2.append("\n词法分析失败：第" + line + "行发生词法错误：" + msg);
	}

	private static void appendToken(Token t, String external) {
		Constants.token.add(t);
		Constants.tokenShow.append(external);
		Constants.tokenShow2.append("(" + t.l + "," + t.i + "," + t.j + ")");
	}

	private static boolean appendWord(String ss, int line) {
		if (ss.length() == 0) {
			return true;
		}
		char first = ss.charAt(0);
		if (Character.isLetter(first)) {
			if (Constants.reservedWord.contains(ss)) {
				int idx = Constants.reservedWord.indexOf(ss);
				appendToken(new Token(2, idx, line), "(2," + ss + ")");
				return true;
			}
			if (isIdentifier(ss)) {
				if (!identifier.contains(ss)) {
					identifier.add(ss);
				}
				int idx = identifier.indexOf(ss);
				appendToken(new Token(3, idx, line), "(3," + ss + ")");
				return true;
			}
			fail(line, "无法识别\"" + ss + "\"");
			return false;
		}
		if (isINTC(ss)) {
			if (!INTC.contains(ss)) {
				INTC.add(ss);
			}
			int idx = INTC.indexOf(ss);
			appendToken(new Token(4, idx, line), "(4," + ss + ")");
			return true;
		}
		fail(line, "无法识别\"" + ss + "\"");
		return false;
	}

	public static boolean doToken(String s) {
		identifier = new ArrayList<String>();
		INTC = new ArrayList<String>();
		CHARC = new ArrayList<String>();
		Constants.token = new ArrayList<Token>();
		Constants.tokenShow = new StringBuffer();
		Constants.tokenShow2 = new StringBuffer();
		int line = 1;
		StringBuffer sb = new StringBuffer();
		Token t;

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (c == '{') {
				if (sb.length() != 0) {
					if (!appendWord(sb.toString(), line)) {
						return false;
					}
					sb = new StringBuffer();
				}
				while (i < s.length() && s.charAt(i) != '}') {
					if (s.charAt(i) == '\n') {
						line++;
					}
					i++;
				}
				if (i >= s.length()) {
					fail(line, "注释缺少结束符\"}\"");
					return false;
				}
				continue;
			}

			if (c == '\'') {
				if (sb.length() != 0) {
					if (!appendWord(sb.toString(), line)) {
						return false;
					}
					sb = new StringBuffer();
				}
				if (i + 2 >= s.length() || s.charAt(i + 2) != '\'') {
					fail(line, "字符常量格式应为'字符'");
					return false;
				}
				String ch = String.valueOf(s.charAt(i + 1));
				if (!CHARC.contains(ch)) {
					CHARC.add(ch);
				}
				int idx = CHARC.indexOf(ch);
				appendToken(new Token(5, idx, line), "(5,'" + ch + "')");
				i += 2;
				continue;
			}

			if (c != ' ' && c != '\n' && c != '\r' && c != '\t'
					&& !Constants.separator.contains(String.valueOf(c))) {
				sb.append(c);
				continue;
			}

			if (sb.length() != 0) {
				if (!appendWord(sb.toString(), line)) {
					return false;
				}
				sb = new StringBuffer();
			}

			if (c == ' ') {
				Constants.tokenShow.append(" ");
				Constants.tokenShow2.append(" ");
				continue;
			}
			if (c == '\r') {
				continue;
			}
			if (c == '\n') {
				line++;
				Constants.tokenShow.append("\n");
				Constants.tokenShow2.append("\n");
				continue;
			}
			if (c == '\t') {
				Constants.tokenShow.append("\t");
				Constants.tokenShow2.append("\t");
				continue;
			}
			if (c == ':') {
				if (i + 1 < s.length() && s.charAt(i + 1) == '=') {
					t = new Token(1, Constants.separator.indexOf(":="), line);
					appendToken(t, "(1,:=)");
					i++;
					continue;
				}
				fail(line, "\":\"后应该接\"=\"");
				return false;
			}
			if (c == '.') {
				if (i + 1 < s.length() && s.charAt(i + 1) == '.') {
					t = new Token(1, Constants.separator.indexOf(".."), line);
					appendToken(t, "(1,..)");
					i++;
					continue;
				}
				t = new Token(1, Constants.separator.indexOf("."), line);
				appendToken(t, "(1,.)");
				if (i + 1 == s.length() || s.charAt(i + 1) == ' ' || s.charAt(i + 1) == '\n'
						|| s.charAt(i + 1) == '\r' || s.charAt(i + 1) == '\t') {
					Constants.tokenShow.append("\n词法分析成功！");
					Constants.tokenShow2.append("\n词法分析成功！");
					return true;
				}
				continue;
			}
			t = new Token(1, Constants.separator.indexOf(String.valueOf(c)), line);
			appendToken(t, "(1," + c + ")");
		}

		if (sb.length() != 0) {
			if (!appendWord(sb.toString(), line)) {
				return false;
			}
		}
		fail(line, "程序未能正常结束");
		return false;
	}
}

