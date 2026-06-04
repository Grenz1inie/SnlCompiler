package com.snl.compiler.core.parser;

import java.util.Stack;

import com.snl.compiler.infrastructure.config.Constants;

public class Parser {
	public static Stack<String> stack;
	public static StringBuffer grammarShow;
	public static StringBuffer match;
	
	public static String doGrammar() {
		grammarShow=new StringBuffer();
		match=new StringBuffer();
		match.append("(");
		stack=new Stack<String>();
		stack.push("Program");
		String iToken=null; // 为token序列首位的还原后的非终极符
		String iStack=null; // 为栈顶非终极符
		int iRule=0; // 为规约用的规则行号
		int iLine=1; // token中的行号
		boolean need=true; // 指示本次循环是否需要还原token，初始时需要，在还原后设为假；在match掉当前非终极符后设为真在continue
		for(int i=0;i<Constants.token.size();){
			if(need){ // 还原token中的单词信息至iToken
				if(Constants.token.get(i).l>iLine){
					iLine=Constants.token.get(i).l;
					grammarShow.append("\n");
				}
				switch(Constants.token.get(i).i){
				case 1:
					iToken=Constants.separator.get(Constants.token.get(i).j);
					break;
				case 2:
					iToken=Constants.reservedWord.get(Constants.token.get(i).j);
					break;
				case 3:
					iToken="ID"; // 赋值为ID，即标识符
					break;
				case 4:
					iToken="INTC"; // 赋值为INTC，即数字常量
					break;
				case 5:
					iToken="CHARC"; // 字符常量
					break;
				default:
				}
				need=false;
			}
			if(stack.isEmpty()){ // 如果token序列还未遍历完而栈已空，则出错
				grammarShow.append("\n语法分析失败！\n第"+iLine+"行进行语法分析时出错：栈已空而token序列不为空！");
				return grammarShow.toString();
			}
			iStack=stack.pop(); // 为栈顶元素
			if(iStack.equals("null")){
				continue;
			}
			if(Constants.nonTerminal.contains(iStack)){ // 栈顶元素为非终极符，则要根据LL(1)分析表进行规约
				if((iRule=Constants.analysis[Constants.nonTerminal.indexOf(iStack)][Constants.terminal.indexOf(iToken)])==0){ // 出错！因没有可用的规则进行规约
					grammarShow.append("\n语法分析失败！\n第"+iLine+"行进行规约时出错：无可用的规则！");
					return grammarShow.toString();
				}else{
					for(int j=Constants.rule.get(iRule).B.size()-1;j>=0;j--){ // 将规则逆序入栈
						 stack.push(Constants.rule.get(iRule).B.get(j)); 
					 }
					 match.append(iRule+",");
					 continue;
				}
			}else{ // 此时栈顶元素为终极符，则要跟token序列首位进行匹配
				if(iStack.equals(iToken)){ // 匹配成功
					match.setLength(match.length()-1); // 删掉末尾的（或，
					if(match.length()!=0){
						match.append(")"); // 如果不空则追加上）
					}
					grammarShow.append(match.toString()+iToken+" ");
					match=new StringBuffer();
					match.append("(");
					i++;
					need=true;
					continue;
				}else{ // 不匹配则出错
					grammarShow.append("\n语法分析失败！\n第"+iLine+"行进行终极符匹配时出错！");
					return grammarShow.toString();
				}
			}
		} // for循环结束
		if(stack.isEmpty()){
			grammarShow.append("\n语法分析成功！");
			return grammarShow.toString();
		}else{
			grammarShow.append("\n语法分析失败！\n第"+iLine+"行进行语法分析时出错：token序列已为空而栈不空！");
			return grammarShow.toString();
		}
	}
}


