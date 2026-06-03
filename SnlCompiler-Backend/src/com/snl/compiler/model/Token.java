package com.snl.compiler.model;

public class Token { // Token类
	public int l; // 行号
	public int i; // i表示类型：1为分隔符，2为保留字，3为标识符，4为数字常量
	public int j; // j表示下标

	public Token(int i, int j,int l) {
		this.i = i;
		this.j = j;
		this.l=l;
	}
}