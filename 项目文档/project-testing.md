# SNL Compiler 项目测试文档

## 1. 测试目标

本文档用于支撑 SNL Compiler 的全场景正确性验证，覆盖词法分析、LL(1) 语法分析、静态配置初始化、模型结构和 前后端交互流程。测试体系按单元测试、集成测试、端到端测试三层组织，要求测试用例可落地、可追溯、可重复执行。

核心质量目标：

- 核心代码覆盖率不低于 90%。核心代码指 `Lexer`、`Parser`、`Constants`、`Token`、`Rule`。
- 词法分析覆盖合法输入、边界输入、非法字符、错误赋值符、前导零数字、程序未正常结束。
- 语法分析覆盖合法程序、缺失关键结构、终结符不匹配、Token 序列提前结束或栈状态异常。
- 集成链路覆盖 `Constants.initialize()`、`Lexer.doToken()`、`Parser.doGrammar()` 的顺序调用。
- 端到端验证覆盖 GUI 启动、源码输入、词法按钮、语法按钮、表示切换、帮助弹窗和状态展示。

## 2. 测试环境

### 2.1 必需环境

| 项目 | 要求 |
| --- | --- |
| JDK | JDK 8 或更高版本 |
| Shell | Windows PowerShell，或等价终端 |
| 编码 | UTF-8 |
| 图形环境 | 端到端 GUI 测试需要桌面环境 |

### 2.2 项目编译

在项目根目录执行：

```powershell
javac -encoding UTF-8 -d bin (Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
```

期望结果：

- 命令退出码为 `0`。
- `bin/com/snl/compiler` 下生成对应 `.class` 文件。
- 控制台无编译错误。

若编译时报 `非法字符: '\ufeff'`，表示源码文件带有 UTF-8 BOM 且当前 JDK 无法识别该文件头。应先将相关 Java 源文件另存为“UTF-8 无 BOM”，再重新执行编译和后续测试命令。

## 3. 测试分层策略

| 层级 | 测试对象 | 目标 | 执行方式 |
| --- | --- | --- | --- |
| 单元测试 | `Lexer`、`Parser`、`Constants`、`Token`、`Rule` | 验证单个类或单个方法的确定性行为 | 临时 Java 测试类直接调用核心方法 |
| 集成测试 | `Constants + Lexer + Parser` | 验证完整编译前端核心链路 | 编译并运行临时集成测试类 |
| 端到端测试 | Vue 前端 + HTTP API | 验证用户视角操作闭环 | 启动前端页面和后端服务进行验证 |

## 4. 覆盖率量化要求

### 4.1 覆盖率指标

| 指标 | 要求 |
| --- | --- |
| 核心代码行覆盖率 | ≥ 90% |
| 核心代码分支覆盖率 | ≥ 85% |
| 词法错误分支覆盖 | 100% 覆盖本文档列出的异常样例 |
| 语法错误分支覆盖 | 100% 覆盖本文档列出的异常样例 |
| 核心业务流程覆盖 | 100% 覆盖“初始化 → 词法分析 → 语法分析 → 结果校验” |

### 4.2 覆盖率统计建议

当前项目未内置 Maven、Gradle 或 JaCoCo。若需要生成正式覆盖率报告，建议后续引入 JaCoCo，并将核心类纳入统计范围。未引入覆盖率工具前，至少通过本文档的测试矩阵证明所有核心业务流程、关键技术节点和异常分支均被样例执行。

核心类覆盖范围：

- `com.snl.compiler.core.lexer.Lexer`
- `com.snl.compiler.core.parser.Parser`
- `com.snl.compiler.infra.config.Constants`
- `com.snl.compiler.model.Token`
- `com.snl.compiler.model.Rule`

## 5. 单元测试

### 5.1 测试范围

单元测试重点验证：

- `Constants.initialize()` 是否正确初始化保留字、分隔符、终结符、非终结符、预测分析表和产生式规则。
- `Lexer.isIdentifier()` 是否正确识别标识符边界。
- `Lexer.isINTC()` 是否正确识别整数常量边界。
- `Lexer.doToken()` 是否正确生成 Token 和错误信息。
- `Parser.doGrammar()` 是否正确处理成功与失败路径。
- `Token`、`Rule` 模型是否能保存内部状态。

### 5.2 极简可运行样例

在项目根目录执行以下 PowerShell 命令，创建并运行临时单元测试类：

```powershell
$testDir = Join-Path $env:TEMP "snl-unit-tests"
New-Item -ItemType Directory -Force $testDir | Out-Null
@'
import com.snl.compiler.core.lexer.Lexer;
import com.snl.compiler.core.parser.Parser;
import com.snl.compiler.infra.config.Constants;
import com.snl.compiler.model.Rule;
import com.snl.compiler.model.Token;

public class UnitSmokeTest {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        Constants.initialize();

        check(Constants.separator.contains(":="), "分隔符表应包含赋值符");
        check(Constants.reservedWord.contains("program"), "保留字表应包含 program");
        check(Constants.terminal.contains("ID"), "终结符表应包含 ID");
        check(Constants.nonTerminal.contains("Program"), "非终结符表应包含 Program");
        check(Constants.rule.size() >= 100, "产生式规则数量应覆盖 SNL 文法");

        check(Lexer.isIdentifier("abc123"), "字母开头的字母数字串应为标识符");
        check(!Lexer.isIdentifier("1abc"), "数字开头不应为标识符");
        check(Lexer.isINTC("0"), "0 应为合法整数常量");
        check(Lexer.isINTC("10"), "10 应为合法整数常量");
        check(!Lexer.isINTC("01"), "前导零整数不应合法");

        Token token = new Token(3, 2, 1);
        check(token.i == 3 && token.j == 2 && token.l == 1, "Token 字段应保存构造参数");

        Rule rule = new Rule();
        rule.A = "Program";
        rule.B.add("ProgramHead");
        check("Program".equals(rule.A) && rule.B.contains("ProgramHead"), "Rule 应保存产生式左右部");

        String validProgram = "program p\n" +
                "type t = integer ;\n" +
                "var t v1;\n" +
                "char v2;\n" +
                "begin\n" +
                "read(v1);\n" +
                "v1:=v1+10;\n" +
                "write(v1)\n" +
                "end.";
        check(Lexer.doToken(validProgram), "合法程序词法分析应成功");
        check(Constants.tokenShow.toString().contains("词法分析成功"), "成功输出应包含词法成功提示");
        check(Parser.doGrammar().contains("语法分析成功"), "合法程序语法分析应成功");

        check(!Lexer.doToken("program p#\nend."), "非法字符应导致词法失败");
        check(Constants.tokenShow.toString().contains("无法识别"), "非法字符应给出无法识别提示");

        check(!Lexer.doToken("program p\nbegin\na:1\nend."), "冒号后非等号应导致词法失败");
        check(Constants.tokenShow.toString().contains("后应该接"), "错误赋值符应给出明确提示");

        check(!Lexer.doToken("program p\nbegin\nend"), "缺少句点应导致程序未正常结束");
        check(Constants.tokenShow.toString().contains("程序未能正常结束"), "缺少结束句点应给出结束失败提示");

        System.out.println("UNIT_TEST_PASS");
    }
}
'@ | Set-Content -Encoding UTF8 (Join-Path $testDir "UnitSmokeTest.java")
javac -encoding UTF-8 -cp bin -d $testDir (Join-Path $testDir "UnitSmokeTest.java")
java -cp "bin;$testDir" UnitSmokeTest
```

期望结果：

- 控制台输出 `UNIT_TEST_PASS`。
- 无 `AssertionError`。
- 非法字符、错误赋值符、缺少末尾句点均被断言覆盖。

### 5.3 单元测试覆盖矩阵

| 用例编号 | 输入或对象 | 覆盖点 | 期望结果 |
| --- | --- | --- | --- |
| UT-01 | `Constants.initialize()` | 静态配置初始化 | 分隔符、保留字、终结符、非终结符、规则非空 |
| UT-02 | `abc123` | 合法标识符 | `Lexer.isIdentifier()` 返回 `true` |
| UT-03 | `1abc` | 非法标识符 | `Lexer.isIdentifier()` 返回 `false` |
| UT-04 | `0`、`10` | 合法整数常量 | `Lexer.isINTC()` 返回 `true` |
| UT-05 | `01` | 前导零边界 | `Lexer.isINTC()` 返回 `false` |
| UT-06 | 简单合法程序 | 词法成功路径 | 返回 `true` 并输出成功提示 |
| UT-07 | 简单合法程序 Token | 语法成功路径 | 输出“语法分析成功” |
| UT-08 | `program p#` | 非法字符分支 | 返回 `false` 并提示无法识别 |
| UT-09 | `a:1` | 错误赋值符分支 | 返回 `false` 并提示 `:` 后应接 `=` |
| UT-10 | 缺少末尾 `.` | 程序未正常结束分支 | 返回 `false` 并提示程序未能正常结束 |
| UT-11 | `Token` | 模型构造 | 字段保存构造参数 |
| UT-12 | `Rule` | 产生式模型 | 左部和右部可正确保存 |

## 6. 集成测试

### 6.1 测试范围

集成测试验证核心业务流程：

```text
Constants.initialize()
  ↓
Lexer.doToken(source)
  ↓
Constants.token / tokenShow / tokenShow2
  ↓
Parser.doGrammar()
  ↓
语法分析结果
```

重点覆盖：

- 合法 SNL 程序完整链路成功。
- 词法失败时不得进入语法分析链路。
- 语法结构错误时词法可成功，但语法应失败。
- 多次连续分析时静态状态可被 `Lexer.doToken()` 重置。

### 6.2 极简可运行样例

在项目根目录执行：

```powershell
$testDir = Join-Path $env:TEMP "snl-integration-tests"
New-Item -ItemType Directory -Force $testDir | Out-Null
@'
import com.snl.compiler.core.lexer.Lexer;
import com.snl.compiler.core.parser.Parser;
import com.snl.compiler.infra.config.Constants;

public class IntegrationSmokeTest {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectLexAndParseSuccess(String source) {
        check(Lexer.doToken(source), "词法分析应成功");
        check(Constants.token.size() > 0, "Token 序列不能为空");
        String grammar = Parser.doGrammar();
        check(grammar.contains("语法分析成功"), "语法分析应成功，实际输出：" + grammar);
    }

    public static void main(String[] args) {
        Constants.initialize();

        String minimalProgram = "program p\n" +
                "type t = integer ;\n" +
                "var t v1;\n" +
                "char v2;\n" +
                "begin\n" +
                "read(v1);\n" +
                "v1:=v1+10;\n" +
                "write(v1)\n" +
                "end.";
        expectLexAndParseSuccess(minimalProgram);

        String syntaxErrorProgram = "program p\n" +
                "begin\n" +
                "read(v1).";
        check(Lexer.doToken(syntaxErrorProgram), "语法错误样例的词法分析应成功");
        check(Parser.doGrammar().contains("语法分析失败"), "缺少 end 应导致语法失败");

        String lexicalErrorProgram = "program p\n" +
                "begin\n" +
                "v1:=01\n" +
                "end.";
        check(!Lexer.doToken(lexicalErrorProgram), "前导零数字应导致词法失败");
        check(Constants.tokenShow.toString().contains("无法识别"), "词法失败应输出错误原因");

        expectLexAndParseSuccess(minimalProgram);

        System.out.println("INTEGRATION_TEST_PASS");
    }
}
'@ | Set-Content -Encoding UTF8 (Join-Path $testDir "IntegrationSmokeTest.java")
javac -encoding UTF-8 -cp bin -d $testDir (Join-Path $testDir "IntegrationSmokeTest.java")
java -cp "bin;$testDir" IntegrationSmokeTest
```

期望结果：

- 控制台输出 `INTEGRATION_TEST_PASS`。
- 合法程序两次连续分析均成功。
- 缺少 `end` 的程序输出“语法分析失败”。
- 前导零数字导致词法失败，并输出无法识别提示。

### 6.3 集成测试覆盖矩阵

| 用例编号 | 场景 | 输入 | 期望结果 |
| --- | --- | --- | --- |
| IT-01 | 完整成功链路 | 简单合法 SNL 程序 | 词法成功，语法成功 |
| IT-02 | 语法异常链路 | 缺少 `end` 的程序 | 词法成功，语法失败 |
| IT-03 | 词法异常链路 | `01` 前导零数字 | 词法失败，不进入语法成功断言 |
| IT-04 | 静态状态重置 | 失败后再次分析合法程序 | 后续合法程序仍成功 |
| IT-05 | Token 内部表示 | 合法程序 | `Constants.token` 非空，内部 Token 可供 Parser 消费 |

## 7. 端到端测试

### 7.1 测试范围

端到端测试从用户视角验证 GUI 闭环：

- 程序可启动并显示窗口。
- 左侧源码输入区可输入 SNL 程序。
- Token 表示下拉框可切换“外部表示”和“内部表示”。
- 点击“词法分析”后输出区显示 Token 或错误信息。
- 词法成功后“语法分析”按钮变为可用。
- 点击“语法分析”后输出区显示推导过程与最终结果。
- 点击“帮助”后弹出说明信息。

### 7.2 极简可运行样例

在项目根目录执行：

```powershell
java -cp bin com.snl.compiler.Main 8080 ../SnlCompiler-Frontend/dist
```

将以下代码粘贴到左侧源码输入区：

```text
program p
type t = integer ;
var t v1;
    char v2;
begin
read(v1);
    v1:=v1+10;
    write(v1)
end.
```

执行步骤：

1. 在下拉框选择“外部表示”。
2. 点击“词法分析”。
3. 检查右侧输出区包含 `(2,program)`、`(3,p)`、`(1,.)` 和“词法分析成功！”。
4. 检查“语法分析”按钮已启用。
5. 点击“语法分析”。
6. 检查右侧输出区末尾包含“语法分析成功！”。
7. 再次选择“内部表示”，点击“词法分析”。
8. 检查输出区包含形如 `(1,2,0)` 的内部 Token 表示。
9. 点击“帮助”，检查弹窗包含 Token 类型说明。

期望结果：

- GUI 全流程无异常退出。
- 状态栏在词法成功后显示“词法分析成功，可进行语法分析。”。
- 状态栏在语法完成后显示“语法分析完成。”。

### 7.3 端到端异常样例

#### E2E-ERR-01 非法字符

输入：

```text
program p#
end.
```

步骤：

1. 点击“词法分析”。
2. 检查输出区包含“词法分析失败”和“无法识别”。
3. 检查“语法分析”按钮不可用。

#### E2E-ERR-02 缺少程序结束句点

输入：

```text
program p
begin
end
```

步骤：

1. 点击“词法分析”。
2. 检查输出区包含“程序未能正常结束”。
3. 检查状态栏提示词法分析失败。

#### E2E-ERR-03 缺少语法关键结构

输入：

```text
program p
begin
read(v1).
```

步骤：

1. 点击“词法分析”，确认词法成功。
2. 点击“语法分析”。
3. 检查输出区包含“语法分析失败”。

## 8. 全场景测试清单

| 类别 | 场景 | 最小样例 | 校验标准 |
| --- | --- | --- | --- |
| 词法成功 | 合法简单程序 | `program p ... end.` | 输出“词法分析成功” |
| 词法成功 | 赋值符 | `v1:=v1+10` | 输出 `(1,:=)` |
| 词法成功 | 数组范围符 | `[1..20]` | 输出 `(1,..)` |
| 词法成功 | 标识符复用 | 多次出现 `v1` | 相同标识符复用同一下标 |
| 词法边界 | 整数 `0` | `v1:=0` | 词法成功 |
| 词法异常 | 前导零 | `v1:=01` | 词法失败，提示无法识别 |
| 词法异常 | 非法字符 | `p#` | 词法失败，提示无法识别 |
| 词法异常 | 错误赋值符 | `a:1` | 词法失败，提示 `:` 后应接 `=` |
| 词法异常 | 缺少末尾句点 | `end` | 词法失败，提示程序未能正常结束 |
| 语法成功 | 标准简单程序 | 本文 E2E 合法样例 | 输出“语法分析成功” |
| 语法成功 | 冒泡排序程序 | `src/TestData.txt` 第二段 | 输出“语法分析成功” |
| 语法异常 | 缺少 `end` | `program p begin read(v1).` | 输出“语法分析失败” |
| 语法异常 | 终结符不匹配 | 删除必要分号或关键字 | 输出“语法分析失败” |
| UI 状态 | 初始状态 | 启动窗口 | 语法分析按钮禁用 |
| UI 状态 | 词法成功后 | 点击词法分析 | 语法分析按钮启用 |
| UI 状态 | 词法失败后 | 输入非法字符 | 语法分析按钮禁用 |

## 9. 回归测试流程

每次修改核心代码后，按以下顺序执行：

1. 编译项目：

```powershell
javac -encoding UTF-8 -d bin (Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
```

2. 运行单元测试样例，确认输出 `UNIT_TEST_PASS`。
3. 运行集成测试样例，确认输出 `INTEGRATION_TEST_PASS`。
4. 启动 GUI，执行端到端合法样例。
5. 执行至少三个端到端异常样例：非法字符、缺少末尾句点、缺少语法关键结构。
6. 若引入覆盖率工具，确认核心代码行覆盖率不低于 90%，分支覆盖率不低于 85%。

## 10. 缺陷定位建议

| 现象 | 优先检查位置 | 排查方向 |
| --- | --- | --- |
| 编译失败 | `Main`、包路径、JDK 编码 | 是否使用 `-encoding UTF-8`，包名与目录是否一致 |
| 编译提示 `非法字符: '\ufeff'` | Java 源文件编码 | 将源码另存为 UTF-8 无 BOM 后重新编译 |
| 词法全部失败 | `Constants.initialize()` | 是否在调用 `Lexer.doToken()` 前初始化配置 |
| 合法标识符失败 | `Lexer.isIdentifier()` | 首字符与后续字符规则是否被修改 |
| 数字边界异常 | `Lexer.isINTC()` | `0` 与前导零分支是否正确 |
| 赋值符无法识别 | `Lexer.doToken()` | `:` 后读取逻辑和 `:=` 分隔符下标是否正确 |
| 语法分析失败 | `Parser.doGrammar()`、`Constants.analysis`、`Constants.rule` | Token 还原、预测分析表和产生式是否一致 |
| 前端阶段按钮状态异常 | `CompilerWorkbench.runStage()` | API 返回值是否正确驱动按钮和结果状态 |

## 11. 可追溯记录模板

建议每次测试记录以下信息：

| 字段 | 示例 |
| --- | --- |
| 测试日期 | 2026-05-27 |
| JDK 版本 | `java -version` 输出 |
| 操作系统 | Windows |
| 代码版本 | 当前提交或本地变更说明 |
| 编译结果 | 通过或失败 |
| 单元测试结果 | `UNIT_TEST_PASS` |
| 集成测试结果 | `INTEGRATION_TEST_PASS` |
| E2E 合法样例 | 通过或失败 |
| E2E 异常样例 | 通过或失败 |
| 覆盖率 | 行覆盖率、分支覆盖率 |
| 遗留问题 | 问题编号或现象描述 |


