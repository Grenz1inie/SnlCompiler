package com.snl.compiler.infra.config;

import java.util.ArrayList;
import java.util.List;

import com.snl.compiler.model.Rule;
import com.snl.compiler.model.Token;

public class Constants {
	public static StringBuffer tokenShow; // 显示token用StringBuffer
	public static StringBuffer tokenShow2; // 测试token用StringBuffer
	public static List<Token> token; // token列表

	public static List<String> separator; // 分隔符列表
	public static List<String> reservedWord; // 保留字列表
	public static List<Character> L; // 大小写字母表
	public static List<Character> D; // 0-9
	public static List<Character> D1; // 1-9

	public static int[][] analysis = new int[68][38]; // LL(1)分析表
	public static List<Rule> rule = new ArrayList<Rule>(); // 存放SNL的104条规则
	public static List<String> terminal=new ArrayList<String>(); // 终极符
	public static List<String> nonTerminal=new ArrayList<String>(); // 非终极符
	
	public static void initialize() { // 初始化各个列表
		separator = new ArrayList<String>();
		reservedWord = new ArrayList<String>();

		separator.add(",");
		separator.add(";");
		separator.add("+");
		separator.add("-");
		separator.add("*");
		separator.add("/");
		separator.add("<");
		separator.add("=");
		separator.add("(");
		separator.add(")");
		separator.add("[");
		separator.add("]");
		separator.add(":=");
		separator.add(".");
		separator.add("..");
		separator.add(":");
		reservedWord.add("program");
		reservedWord.add("type");
		reservedWord.add("var");
		reservedWord.add("integer");
		reservedWord.add("char");
		reservedWord.add("array");
		reservedWord.add("record");
		reservedWord.add("of");
		reservedWord.add("procedure");
		reservedWord.add("begin");
		reservedWord.add("while");
		reservedWord.add("do");
		reservedWord.add("if");
		reservedWord.add("then");
		reservedWord.add("else");
		reservedWord.add("fi");
		reservedWord.add("endwh");
		reservedWord.add("end");
		reservedWord.add("read");
		reservedWord.add("write");
		reservedWord.add("return");
		
		L = new ArrayList<Character>();
		D = new ArrayList<Character>();
		D1 = new ArrayList<Character>();
		L.add('A');
		L.add('B');
		L.add('C');
		L.add('D');
		L.add('E');
		L.add('F');
		L.add('G');
		L.add('H');
		L.add('I');
		L.add('J');
		L.add('K');
		L.add('L');
		L.add('M');
		L.add('N');
		L.add('O');
		L.add('P');
		L.add('Q');
		L.add('R');
		L.add('S');
		L.add('T');
		L.add('U');
		L.add('V');
		L.add('W');
		L.add('X');
		L.add('Y');
		L.add('Z');
		L.add('a');
		L.add('b');
		L.add('c');
		L.add('d');
		L.add('e');
		L.add('f');
		L.add('g');
		L.add('h');
		L.add('i');
		L.add('j');
		L.add('k');
		L.add('l');
		L.add('m');
		L.add('n');
		L.add('o');
		L.add('p');
		L.add('q');
		L.add('r');
		L.add('s');
		L.add('t');
		L.add('u');
		L.add('v');
		L.add('w');
		L.add('x');
		L.add('y');
		L.add('z');
		D.add('0');
		D.add('1');
		D.add('2');
		D.add('3');
		D.add('4');
		D.add('5');
		D.add('6');
		D.add('7');
		D.add('8');
		D.add('9');
		D1.add('1');
		D1.add('2');
		D1.add('3');
		D1.add('4');
		D1.add('5');
		D1.add('6');
		D1.add('7');
		D1.add('8');
		D1.add('9');
		
		terminal.add("null");
		terminal.add("program");
		terminal.add("type");
		terminal.add("integer");
		terminal.add("char");
		terminal.add("array");
		terminal.add("INTC");
		terminal.add("record");
		terminal.add("end");
		terminal.add("var");
		terminal.add("procedure");
		terminal.add("begin");
		terminal.add("if");
		terminal.add("then");
		terminal.add("else");
		terminal.add("fi");
		terminal.add("while");
		terminal.add("do");
		terminal.add("endwh");
		terminal.add("read");
		terminal.add("write");
		terminal.add("return");
		terminal.add("ID");
		terminal.add(".");
		terminal.add(";");
		terminal.add(",");
		terminal.add("(");
		terminal.add(")");
		terminal.add("[");
		terminal.add("]");
		terminal.add("<");
		terminal.add("=");
		terminal.add("+");
		terminal.add("-");
		terminal.add("*");
		terminal.add("/");
		terminal.add(":=");
		terminal.add("CHARC");
		
		nonTerminal.add("null");
		nonTerminal.add("Program");
		nonTerminal.add("ProgramHead");
		nonTerminal.add("ProgramName");
		nonTerminal.add("DeclarePart");
		nonTerminal.add("TypeDecpart");
		nonTerminal.add("TypeDec");
		nonTerminal.add("TypeDecList");
		nonTerminal.add("TypeDecMore");
		nonTerminal.add("TypeId");
		nonTerminal.add("TypeDef");
		nonTerminal.add("BaseType");
		nonTerminal.add("StructureType");
		nonTerminal.add("ArrayType");
		nonTerminal.add("Low");
		nonTerminal.add("Top");
		nonTerminal.add("RecType");
		nonTerminal.add("FieldDecList");
		nonTerminal.add("FieldDecMore");
		nonTerminal.add("IdList");
		nonTerminal.add("IdMore");
		nonTerminal.add("VarDecpart");
		nonTerminal.add("VarDec");
		nonTerminal.add("VarDecList");
		nonTerminal.add("VarDecMore");
		nonTerminal.add("VarIdList");
		nonTerminal.add("VarIdMore");
		nonTerminal.add("ProcDecpart");
		nonTerminal.add("ProcDec");
		nonTerminal.add("ProcDecMore");
		nonTerminal.add("ProcName");
		nonTerminal.add("ParamList");
		nonTerminal.add("ParamDecList");
		nonTerminal.add("ParamMore");
		nonTerminal.add("Param");
		nonTerminal.add("FormList");
		nonTerminal.add("FidMore");
		nonTerminal.add("ProcDecPart");
		nonTerminal.add("ProcBody");
		nonTerminal.add("ProgramBody");
		nonTerminal.add("StmList");
		nonTerminal.add("StmMore");
		nonTerminal.add("Stm");
		nonTerminal.add("AssCall");
		nonTerminal.add("AssignmentRest");
		nonTerminal.add("ConditionalStm");
		nonTerminal.add("LoopStm");
		nonTerminal.add("InputStm");
		nonTerminal.add("Invar");
		nonTerminal.add("OutputStm");
		nonTerminal.add("ReturnStm");
		nonTerminal.add("CallStmRest");
		nonTerminal.add("ActParamList");
		nonTerminal.add("ActParamMore");
		nonTerminal.add("RelExp");
		nonTerminal.add("OtherRelE");
		nonTerminal.add("Exp");
		nonTerminal.add("OtherTerm");
		nonTerminal.add("Term");
		nonTerminal.add("OtherFactor");
		nonTerminal.add("Factor");
		nonTerminal.add("Variable");
		nonTerminal.add("VariMore");
		nonTerminal.add("FieldVar");
		nonTerminal.add("FieldVarMore");
		nonTerminal.add("CmpOp");
		nonTerminal.add("AddOp");
		nonTerminal.add("MulOp");
		
		analysis[1][1] = 1;
		analysis[2][1] = 2;
		analysis[3][22] = 3;

		analysis[4][2] = 4;
		analysis[4][9] = 4;
		analysis[4][10] = 4;
		analysis[4][11] = 4;

		analysis[5][2] = 6;
		analysis[5][9] = 5;
		analysis[5][10] = 5;
		analysis[5][11] = 5;

		analysis[6][2] = 7;
		analysis[7][22] = 8;

		analysis[8][9] = 9;
		analysis[8][10] = 9;
		analysis[8][11] = 9;
		analysis[8][22] = 10;

		analysis[9][22] = 11;

		analysis[10][3] = 12;
		analysis[10][4] = 12;
		analysis[10][5] = 13;
		analysis[10][7] = 13;
		analysis[10][22] = 14;

		analysis[11][3] = 15;
		analysis[11][4] = 16;

		analysis[12][5] = 17;
		analysis[12][7] = 18;

		analysis[13][5] = 19;
		analysis[14][6] = 20;
		analysis[15][6] = 21;
		analysis[16][7] = 22;

		analysis[17][3] = 23;
		analysis[17][4] = 23;
		analysis[17][5] = 24;

		analysis[18][3] = 26;
		analysis[18][4] = 26;
		analysis[18][5] = 26;
		analysis[18][8] = 25;

		analysis[19][22] = 27;

		analysis[20][24] = 28;
		analysis[20][25] = 29;

		analysis[21][9] = 31;
		analysis[21][10] = 30;
		analysis[21][11] = 30;

		analysis[22][9] = 32;

		analysis[23][3] = 33;
		analysis[23][4] = 33;
		analysis[23][5] = 33;
		analysis[23][7] = 33;
		analysis[23][22] = 33;

		analysis[24][3] = 35;
		analysis[24][4] = 35;
		analysis[24][5] = 35;
		analysis[24][7] = 35;
		analysis[24][10] = 34;
		analysis[24][11] = 34;
		analysis[24][22] = 35;

		analysis[25][22] = 36;

		analysis[26][24] = 37;
		analysis[26][25] = 38;

		analysis[27][10] = 40;
		analysis[27][11] = 39;

		analysis[28][10] = 41;

		analysis[29][10] = 43;
		analysis[29][11] = 42;

		analysis[30][22] = 44;

		analysis[31][3] = 46;
		analysis[31][4] = 46;
		analysis[31][5] = 46;
		analysis[31][7] = 46;
		analysis[31][9] = 46;
		analysis[31][22] = 46;
		analysis[31][27] = 45;

		analysis[32][3] = 47;
		analysis[32][4] = 47;
		analysis[32][5] = 47;
		analysis[32][7] = 47;
		analysis[32][9] = 47;
		analysis[32][22] = 47;

		analysis[33][24] = 49;
		analysis[33][27] = 48;

		analysis[34][3] = 50;
		analysis[34][4] = 50;
		analysis[34][5] = 50;
		analysis[34][7] = 50;
		analysis[34][9] = 51;
		analysis[34][22] = 50;

		analysis[35][22] = 52;

		analysis[36][24] = 53;
		analysis[36][25] = 54;
		analysis[36][27] = 53;

		analysis[37][2] = 55;
		analysis[37][9] = 55;
		analysis[37][10] = 55;
		analysis[37][11] = 55;

		analysis[38][11] = 56;
		analysis[39][11] = 57;

		analysis[40][12] = 58;
		analysis[40][16] = 58;
		analysis[40][19] = 58;
		analysis[40][20] = 58;
		analysis[40][21] = 58;
		analysis[40][22] = 58;

		analysis[41][8] = 59;
		analysis[41][14] = 59;
		analysis[41][15] = 59;
		analysis[41][18] = 59;
		analysis[41][24] = 60;

		analysis[42][12] = 61;
		analysis[42][16] = 62;
		analysis[42][19] = 63;
		analysis[42][20] = 64;
		analysis[42][21] = 65;
		analysis[42][22] = 66;

		analysis[43][23] = 67;
		analysis[43][26] = 68;
		analysis[43][28] = 67;
		analysis[43][36] = 67;

		analysis[44][23] = 69;
		analysis[44][28] = 69;
		analysis[44][36] = 69;

		analysis[45][12] = 70;
		analysis[46][16] = 71;
		analysis[47][19] = 72;
		analysis[48][22] = 73;
		analysis[49][20] = 74;
		analysis[50][21] = 75;
		analysis[51][26] = 76;

		analysis[52][6] = 78;
		analysis[52][22] = 78;
		analysis[52][26] = 78;
		analysis[52][27] = 77;
		analysis[52][37] = 78;

		analysis[53][25] = 80;
		analysis[53][27] = 79;

		analysis[54][6] = 81;
		analysis[54][22] = 81;
		analysis[54][26] = 81;
		analysis[54][37] = 81;

		analysis[55][30] = 82;
		analysis[55][31] = 82;

		analysis[56][6] = 83;
		analysis[56][22] = 83;
		analysis[56][26] = 83;
		analysis[56][37] = 83;

		analysis[57][8] = 84;
		analysis[57][13] = 84;
		analysis[57][14] = 84;
		analysis[57][15] = 84;
		analysis[57][17] = 84;
		analysis[57][18] = 84;
		analysis[57][24] = 84;
		analysis[57][25] = 84;
		analysis[57][27] = 84;
		analysis[57][29] = 84;
		analysis[57][30] = 84;
		analysis[57][31] = 84;
		analysis[57][32] = 85;
		analysis[57][33] = 85;
		
		analysis[58][6] = 86;
		analysis[58][22] = 86;
		analysis[58][26] = 86;
		analysis[58][37] = 86;
		
		analysis[59][8] = 87;
		analysis[59][13] = 87;
		analysis[59][14] = 87;
		analysis[59][15] = 87;
		analysis[59][17] = 87;
		analysis[59][18] = 87;
		analysis[59][24] = 87;
		analysis[59][25] = 87;
		analysis[59][27] = 87;
		analysis[59][29] = 87;
		analysis[59][30] = 87;
		analysis[59][31] = 87;
		analysis[59][32] = 87;
		analysis[59][33] = 87;
		analysis[59][34] = 88;
		analysis[59][35] = 88;

		analysis[60][6] = 90;
		analysis[60][22] = 91;
		analysis[60][26] = 89;
		analysis[60][37] = 105;

		analysis[61][22] = 92;

		analysis[62][8] = 93;
		analysis[62][13] = 93;
		analysis[62][14] = 93;
		analysis[62][15] = 93;
		analysis[62][17] = 93;
		analysis[62][18] = 93;
		analysis[62][23] = 95;
		analysis[62][24] = 93;
		analysis[62][25] = 93;
		analysis[62][27] = 93;
		analysis[62][28] = 94;
		analysis[62][29] = 93;
		analysis[62][30] = 93;
		analysis[62][31] = 93;
		analysis[62][32] = 93;
		analysis[62][33] = 93;
		analysis[62][34] = 93;
		analysis[62][35] = 93;
		analysis[62][36] = 93;
		
		analysis[63][22] = 96;
		
		analysis[64][8] = 97;
		analysis[64][13] = 97;
		analysis[64][14] = 97;
		analysis[64][15] = 97;
		analysis[64][17] = 97;
		analysis[64][18] = 97;
		analysis[64][24] = 97;
		analysis[64][25] = 97;
		analysis[64][27] = 97;
		analysis[64][28] = 98;
		analysis[64][29] = 97;
		analysis[64][30] = 97;
		analysis[64][31] = 97;
		analysis[64][32] = 97;
		analysis[64][33] = 97;
		analysis[64][34] = 97;
		analysis[64][35] = 97;
		analysis[64][36] = 97;
		
		analysis[65][30] = 99;
		analysis[65][31] = 100;
		
		analysis[66][32] = 101;
		analysis[66][33] = 102;
		
		analysis[67][34] = 103;
		analysis[67][35] = 104;
		
		Rule r = new Rule(); // 填充下标为零的规则
		r.A = "SNL";
		r.B.add(".");
		rule.add(r);

		r = new Rule();
		r.A = "Program";
		r.B.add("ProgramHead");
		r.B.add("DeclarePart");
		r.B.add("ProgramBody");
		r.B.add(".");
		rule.add(r);

		r = new Rule();
		r.A = "ProgramHead";
		r.B.add("program");
		r.B.add("ProgramName");
		rule.add(r);

		r = new Rule();
		r.A = "ProgramName";
		r.B.add("ID");
		rule.add(r);

		r = new Rule();
		r.A = "DeclarePart";
		r.B.add("TypeDecpart");
		r.B.add("VarDecpart");
		r.B.add("ProcDecpart");
		rule.add(r);

		r = new Rule();
		r.A = "TypeDecpart";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "TypeDecpart";
		r.B.add("TypeDec");
		rule.add(r);

		r = new Rule();
		r.A = "TypeDec";
		r.B.add("type");
		r.B.add("TypeDecList");
		rule.add(r);

		r = new Rule();
		r.A = "TypeDecList";
		r.B.add("TypeId");
		r.B.add("=");
		r.B.add("TypeDef");
		r.B.add(";");
		r.B.add("TypeDecMore");
		rule.add(r);

		r = new Rule();
		r.A = "TypeDecMore";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "TypeDecMore";
		r.B.add("TypeDecList");
		rule.add(r);

		r = new Rule();
		r.A = "TypeId";
		r.B.add("ID");
		rule.add(r);

		r = new Rule();
		r.A = "TypeDef";
		r.B.add("BaseType");
		rule.add(r);

		r = new Rule();
		r.A = "TypeDef";
		r.B.add("StructureType");
		rule.add(r);

		r = new Rule();
		r.A = "TypeDef";
		r.B.add("ID");
		rule.add(r);

		r = new Rule();
		r.A = "BaseType";
		r.B.add("integer");
		rule.add(r);

		r = new Rule();
		r.A = "BaseType";
		r.B.add("char");
		rule.add(r);

		r = new Rule();
		r.A = "StructureType";
		r.B.add("ArrayType");
		rule.add(r);

		r = new Rule();
		r.A = "StructureType";
		r.B.add("RecType");
		rule.add(r);

		r = new Rule();
		r.A = "ArrayType";
		r.B.add("array");
		r.B.add("[");
		r.B.add("Low");
		r.B.add("..");
		r.B.add("Top");
		r.B.add("]");
		r.B.add("of");
		r.B.add("BaseType");
		rule.add(r);

		r = new Rule();
		r.A = "Low";
		r.B.add("INTC");
		rule.add(r);

		r = new Rule();
		r.A = "Top";
		r.B.add("INTC");
		rule.add(r);

		r = new Rule();
		r.A = "RecType";
		r.B.add("record");
		r.B.add("FieldDecList");
		r.B.add("end");
		rule.add(r);

		r = new Rule();
		r.A = "FieldDecList";
		r.B.add("BaseType");
		r.B.add("IdList");
		r.B.add(";");
		r.B.add("FieldDecMore");
		rule.add(r);

		r = new Rule();
		r.A = "FieldDecList";
		r.B.add("ArrayType");
		r.B.add("IdList");
		r.B.add(";");
		r.B.add("FieldDecMore");
		rule.add(r);

		r = new Rule();
		r.A = "FieldDecMore";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "FieldDecMore";
		r.B.add("FieldDecList");
		rule.add(r);

		r = new Rule();
		r.A = "IdList";
		r.B.add("ID");
		r.B.add("IdMore");
		rule.add(r);

		r = new Rule();
		r.A = "IdMore";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "IdMore";
		r.B.add(",");
		r.B.add("IdList");
		rule.add(r);

		r = new Rule();
		r.A = "VarDecpart";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "VarDecpart";
		r.B.add("VarDec");
		rule.add(r);

		r = new Rule();
		r.A = "VarDec";
		r.B.add("var");
		r.B.add("VarDecList");
		rule.add(r);

		r = new Rule();
		r.A = "VarDecList";
		r.B.add("TypeDef");
		r.B.add("VarIdList");
		r.B.add(";");
		r.B.add("VarDecMore");
		rule.add(r);

		r = new Rule();
		r.A = "VarDecMore";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "VarDecMore";
		r.B.add("VarDecList");
		rule.add(r);

		r = new Rule();
		r.A = "VarIdList";
		r.B.add("ID");
		r.B.add("VarIdMore");
		rule.add(r);

		r = new Rule();
		r.A = "VarIdMore";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "VarIdMore";
		r.B.add(",");
		r.B.add("VarIdList");
		rule.add(r);

		r = new Rule();
		r.A = "ProcDecpart";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "ProcDecpart";
		r.B.add("ProcDec");
		rule.add(r);

		r = new Rule();
		r.A = "ProcDec";
		r.B.add("procedure");
		r.B.add("ProcName");
		r.B.add("(");
		r.B.add("ParamList");
		r.B.add(")");
		r.B.add(";");
		r.B.add("ProcDecPart");
		r.B.add("ProcBody");
		r.B.add("ProcDecMore");
		rule.add(r);

		r = new Rule();
		r.A = "ProcDecMore";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "ProcDecMore";
		r.B.add("ProcDec");
		rule.add(r);

		r = new Rule();
		r.A = "ProcName";
		r.B.add("ID");
		rule.add(r);

		r = new Rule();
		r.A = "ParamList";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "ParamList";
		r.B.add("ParamDecList");
		rule.add(r);

		r = new Rule();
		r.A = "ParamDecList";
		r.B.add("Param");
		r.B.add("ParamMore");
		rule.add(r);

		r = new Rule();
		r.A = "ParamMore";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "ParamMore";
		r.B.add(";");
		r.B.add("ParamDecList");
		rule.add(r);

		r = new Rule();
		r.A = "Param";
		r.B.add("TypeDef");
		r.B.add("FormList");
		rule.add(r);

		r = new Rule();
		r.A = "Param";
		r.B.add("var");
		r.B.add("TypeDef");
		r.B.add("FormList");
		rule.add(r);

		r = new Rule();
		r.A = "FormList";
		r.B.add("ID");
		r.B.add("FidMore");
		rule.add(r);

		r = new Rule();
		r.A = "FidMore";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "FidMore";
		r.B.add(",");
		r.B.add("FormList");
		rule.add(r);

		r = new Rule();
		r.A = "ProcDecPart";
		r.B.add("DeclarePart");
		rule.add(r);

		r = new Rule();
		r.A = "ProcBody";
		r.B.add("ProgramBody");
		rule.add(r);

		r = new Rule();
		r.A = "ProgramBody";
		r.B.add("begin");
		r.B.add("StmList");
		r.B.add("end");
		rule.add(r);

		r = new Rule();
		r.A = "StmList";
		r.B.add("Stm");
		r.B.add("StmMore");
		rule.add(r);

		r = new Rule();
		r.A = "StmMore";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "StmMore";
		r.B.add(";");
		r.B.add("StmList");
		rule.add(r);

		r = new Rule();
		r.A = "Stm";
		r.B.add("ConditionalStm");
		rule.add(r);

		r = new Rule();
		r.A = "Stm";
		r.B.add("LoopStm");
		rule.add(r);

		r = new Rule();
		r.A = "Stm";
		r.B.add("InputStm");
		rule.add(r);

		r = new Rule();
		r.A = "Stm";
		r.B.add("OutputStm");
		rule.add(r);

		r = new Rule();
		r.A = "Stm";
		r.B.add("ReturnStm");
		rule.add(r);

		r = new Rule();
		r.A = "Stm";
		r.B.add("ID");
		r.B.add("AssCall");
		rule.add(r);

		r = new Rule();
		r.A = "AssCall";
		r.B.add("AssignmentRest");
		rule.add(r);

		r = new Rule();
		r.A = "AssCall";
		r.B.add("CallStmRest");
		rule.add(r);

		r = new Rule();
		r.A = "AssignmentRest";
		r.B.add("VariMore");
		r.B.add(":=");
		r.B.add("Exp");
		rule.add(r);

		r = new Rule();
		r.A = "ConditionalStm";
		r.B.add("if");
		r.B.add("RelExp");
		r.B.add("then");
		r.B.add("StmList");
		r.B.add("else");
		r.B.add("StmList");
		r.B.add("fi");
		rule.add(r);

		r = new Rule();
		r.A = "LoopStm";
		r.B.add("while");
		r.B.add("RelExp");
		r.B.add("do");
		r.B.add("StmList");
		r.B.add("endwh");
		rule.add(r);

		r = new Rule();
		r.A = "InputStm";
		r.B.add("read");
		r.B.add("(");
		r.B.add("Invar");
		r.B.add(")");
		rule.add(r);

		r = new Rule();
		r.A = "Invar";
		r.B.add("ID");
		rule.add(r);

		r = new Rule();
		r.A = "OutputStm";
		r.B.add("write");
		r.B.add("(");
		r.B.add("Exp");
		r.B.add(")");
		rule.add(r);

		r = new Rule();
		r.A = "ReturnStm";
		r.B.add("return");
		r.B.add("(");
		r.B.add("Exp");
		r.B.add(")");
		rule.add(r);

		r = new Rule();
		r.A = "CallStmRest";
		r.B.add("(");
		r.B.add("ActParamList");
		r.B.add(")");
		rule.add(r);

		r = new Rule();
		r.A = "ActParamList";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "ActParamList";
		r.B.add("Exp");
		r.B.add("ActParamMore");
		rule.add(r);

		r = new Rule();
		r.A = "ActParamMore";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "ActParamMore";
		r.B.add(",");
		r.B.add("ActParamList");
		rule.add(r);

		r = new Rule();
		r.A = "RelExp";
		r.B.add("Exp");
		r.B.add("OtherRelE");
		rule.add(r);

		r = new Rule();
		r.A = "OtherRelE";
		r.B.add("CmpOp");
		r.B.add("Exp");
		rule.add(r);

		r = new Rule();
		r.A = "Exp";
		r.B.add("Term");
		r.B.add("OtherTerm");
		rule.add(r);

		r = new Rule();
		r.A = "OtherTerm";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "OtherTerm";
		r.B.add("AddOp");
		r.B.add("Exp");
		rule.add(r);

		r = new Rule();
		r.A = "Term";
		r.B.add("Factor");
		r.B.add("OtherFactor");
		rule.add(r);

		r = new Rule();
		r.A = "OtherFactor";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "OtherFactor";
		r.B.add("MulOp");
		r.B.add("Term");
		rule.add(r);

		r = new Rule();
		r.A = "Factor";
		r.B.add("(");
		r.B.add("Exp");
		r.B.add(")");
		rule.add(r);

		r = new Rule();
		r.A = "Factor";
		r.B.add("INTC");
		rule.add(r);

		r = new Rule();
		r.A = "Factor";
		r.B.add("Variable");
		rule.add(r);

		r = new Rule();
		r.A = "Variable";
		r.B.add("ID");
		r.B.add("VariMore");
		rule.add(r);

		r = new Rule();
		r.A = "VariMore";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "VariMore";
		r.B.add("[");
		r.B.add("Exp");
		r.B.add("]");
		rule.add(r);

		r = new Rule();
		r.A = "VariMore";
		r.B.add(".");
		r.B.add("FieldVar");
		rule.add(r);

		r = new Rule();
		r.A = "FieldVar";
		r.B.add("ID");
		r.B.add("FieldVarMore");
		rule.add(r);

		r = new Rule();
		r.A = "FieldVarMore";
		r.B.add("null");
		rule.add(r);

		r = new Rule();
		r.A = "FieldVarMore";
		r.B.add("[");
		r.B.add("Exp");
		r.B.add("]");
		rule.add(r);

		r = new Rule();
		r.A = "CmpOp";
		r.B.add("<");
		rule.add(r);

		r = new Rule();
		r.A = "CmpOp";
		r.B.add("=");
		rule.add(r);

		r = new Rule();
		r.A = "AddOp";
		r.B.add("+");
		rule.add(r);

		r = new Rule();
		r.A = "AddOp";
		r.B.add("-");
		rule.add(r);

		r = new Rule();
		r.A = "MulOp";
		r.B.add("*");
		rule.add(r);

		r = new Rule();
		r.A = "MulOp";
		r.B.add("/");
		rule.add(r);

		r = new Rule();
		r.A = "Factor";
		r.B.add("CHARC");
		rule.add(r);
	}
}

