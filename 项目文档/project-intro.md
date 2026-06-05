# SNL Compiler 项目介绍文档

## 1. 项目定位

SNL Compiler 是一个面向编译原理课程实验的 SNL（Small Nested Language）编译器教学项目。当前仓库采用前后端分离结构：后端基于 Spring Boot 提供 HTTP API，前端基于 Vue 3 提供可视化工作台。

项目覆盖的核心流程不再局限于“词法 + LL(1) 语法分析”。按当前实现，系统已经支持：

- 词法分析
- LL(1) 语法分析
- 递归下降语法分析
- 语义分析
- MIPS 目标代码生成

它更适合作为课程实验、课堂演示、SNL 文法学习和编译前端扩展的基础工程，而不是只展示单一分析阶段的极简原型。

## 2. 设计目标

- 用统一工作台串联词法、语法、语义和代码生成阶段。
- 保留课程实验可读性，让 `Constants`、`Lexer`、`Parser`、`RecursiveDescentParser` 等核心实现仍便于对照教材。
- 通过 HTTP API 解耦前后端，降低界面迭代成本。
- 同时输出 Token、语法树、符号表和 MIPS 文本，强化可视化与可验证性。

## 3. 核心能力

### 3.1 词法分析

词法分析由 `com.snl.compiler.core.lexer.Lexer` 提供，入口为 `Lexer.doToken(String source)`。

当前实现支持：

- Token 外部表示 `(类型,词素)` 与内部表示 `(行号,类型,下标)`
- 保留字、标识符、整数常量、字符常量识别
- `:=`、`..`、数组下标、记录相关符号识别
- `{ ... }` 注释跳过
- 非法字符、错误赋值符、缺少结束句点等词法错误提示

Token 类型约定如下：

| 类型编号 | 含义 |
| --- | --- |
| 1 | 分隔符或运算符 |
| 2 | 保留字 |
| 3 | 标识符 |
| 4 | 整数常量 |
| 5 | 字符常量 |

### 3.2 LL(1) 语法分析

LL(1) 语法分析由 `com.snl.compiler.core.parser.Parser` 提供，入口为 `Parser.doGrammar()`。

当前实现特点：

- 基于 `Constants.analysis` 预测分析表和 `Constants.rule` 产生式集合
- 将标识符统一映射为 `ID`，整数常量映射为 `INTC`，字符常量映射为 `CHARC`
- 输出规约过程与中文错误信息
- 在 LL(1) 分析后，后端还会进一步构建 AST 并生成语法树可视化数据

### 3.3 递归下降语法分析

递归下降分析由 `com.snl.compiler.core.parser.RecursiveDescentParser` 提供。

当前实现支持：

- 程序头、类型声明、变量声明、过程声明
- 数组类型与 `a[exp]`
- `record ... end` 记录类型与 `a.field`
- `if`、`while`、`read`、`write`、`return`
- 构建 AST，供后续语义分析和代码生成复用

### 3.4 语义分析

语义分析由 `com.snl.compiler.core.semantic.SemanticAnalyzer` 提供。

当前实现支持：

- 符号表建立与作用域管理
- 重复定义检查
- 未声明标识符检查
- 类型名 / 过程名误用检查
- 赋值类型兼容性检查
- 输出符号表文本结果

### 3.5 MIPS 目标代码生成

MIPS 生成由 `com.snl.compiler.application.codegen.MipsCodeGenerator` 提供，经 `CompilerPipeline.codegen(...)` 调用。

当前实现可为部分 SNL 程序生成 MIPS 汇编，适合在 MARS 或 QtSpim 中进一步验证。需要注意：

- 代码生成依赖递归下降语法分析成功且语义分析无错误
- 当前对部分复杂结构仍有限制，例如记录域访问存在未完全支持的场景
- 生成阶段会返回提示信息，而不总是保证完整覆盖全部语法特性

## 4. 前后端结构

### 4.1 后端

后端入口是 `com.snl.compiler.CompilerApplication`，基于 Spring Boot 启动。

HTTP API 入口为 `com.snl.compiler.api.CompilerController`，提供以下接口：

- `POST /api/compile/lexical`
- `POST /api/compile/grammar`
- `POST /api/compile/recursive`
- `POST /api/compile/semantic`
- `POST /api/compile/codegen`

流程编排集中在 `com.snl.compiler.application.CompilerPipeline`：

- `lexical()` 负责词法分析
- `grammar()` 负责 LL(1) 分析并补充语法树
- `recursive()` 负责递归下降与 AST 构建
- `semantic()` 负责语义检查与符号表输出
- `codegen()` 负责在语义通过后生成 MIPS

### 4.2 前端

前端主界面位于 `SnlCompiler-Frontend/src/components/CompilerWorkbench.vue`。

当前工作台提供：

- 源码输入区
- 外部 / 内部 Token 表示切换
- 阶段选择器：词法、LL(1)、递归下降、语义、MIPS
- 运行按钮
- 语法树图形展示
- 词素表展示
- 文件导入、示例填充、清空操作

当前界面不是旧版文档里描述的“帮助按钮 + 状态栏 + 多个独立阶段按钮”形态，而是统一阶段切换式工作台。

## 5. 模块结构

### 5.1 后端主要文件

| 路径 | 职责 |
| --- | --- |
| `SnlCompiler-Backend/src/main/java/com/snl/compiler/CompilerApplication.java` | Spring Boot 启动入口 |
| `SnlCompiler-Backend/src/main/java/com/snl/compiler/api/CompilerController.java` | 编译 HTTP API |
| `SnlCompiler-Backend/src/main/java/com/snl/compiler/application/CompilerService.java` | 服务层封装 |
| `SnlCompiler-Backend/src/main/java/com/snl/compiler/application/CompilerPipeline.java` | 五个编译阶段的统一编排 |
| `SnlCompiler-Backend/src/main/java/com/snl/compiler/core/lexer/Lexer.java` | 词法分析 |
| `SnlCompiler-Backend/src/main/java/com/snl/compiler/core/parser/Parser.java` | LL(1) 语法分析 |
| `SnlCompiler-Backend/src/main/java/com/snl/compiler/core/parser/RecursiveDescentParser.java` | 递归下降语法分析 |
| `SnlCompiler-Backend/src/main/java/com/snl/compiler/core/semantic/SemanticAnalyzer.java` | 语义分析 |
| `SnlCompiler-Backend/src/main/java/com/snl/compiler/application/codegen/MipsCodeGenerator.java` | MIPS 代码生成 |
| `SnlCompiler-Backend/src/main/java/com/snl/compiler/infrastructure/config/Constants.java` | 文法、终结符、预测分析表、共享静态状态 |
| `SnlCompiler-Backend/src/main/java/com/snl/compiler/domain/token/Token.java` | Token 模型 |
| `SnlCompiler-Backend/src/main/java/com/snl/compiler/domain/grammar/Rule.java` | 产生式模型 |

### 5.2 前端主要文件

| 路径 | 职责 |
| --- | --- |
| `SnlCompiler-Frontend/src/components/CompilerWorkbench.vue` | 主工作台 |
| `SnlCompiler-Frontend/src/api/compiler.ts` | API 请求与响应类型 |

### 5.3 示例程序

示例 SNL 文件位于：

- `SnlCompiler-Backend/src/main/resources/samples/sample.snl`
- `SnlCompiler-Backend/src/main/resources/samples/comment_test.snl`
- `SnlCompiler-Backend/src/main/resources/samples/char_test.snl`
- `SnlCompiler-Backend/src/main/resources/samples/record_test.snl`
- `SnlCompiler-Backend/src/main/resources/samples/bubble_sort.snl`

## 6. 运行时数据流

```text
用户在 CompilerWorkbench 输入或导入 SNL 源码
  ↓
前端调用 /api/compile/{stage}
  ↓
CompilerController -> CompilerService -> CompilerPipeline
  ↓
词法分析：Lexer.doToken(...)
  ↓
按阶段进入 LL(1) / 递归下降 / 语义 / MIPS
  ↓
返回 Token、错误信息、语法树、符号表或 MIPS 文本
  ↓
前端展示结果、语法树图、词素表
```

## 7. 技术栈与环境要求

| 类别 | 选型 | 说明 |
| --- | --- | --- |
| 后端语言 | Java 21 | 由 Maven 管理构建 |
| 后端框架 | Spring Boot 4 | 提供 HTTP API |
| 前端语言 | TypeScript | Vue 单页应用 |
| 前端框架 | Vue 3 + Element Plus | 工作台界面 |
| 图可视化 | AntV X6 | 语法树绘制 |
| 前端构建 | Vite | 开发与打包 |
| 后端构建 | Maven | 见 `pom.xml` |

环境建议：

- JDK 21
- Node.js 20 或更高版本
- Windows、macOS、Linux 均可
- UTF-8 编码

## 8. 构建与运行

### 8.1 后端启动

在 `SnlCompiler-Backend` 目录执行：

```powershell
./mvnw spring-boot:run
```

或：

```powershell
./mvnw test
./mvnw package
java -jar target/compiler-0.0.1-SNAPSHOT.jar
```

### 8.2 前端启动

在 `SnlCompiler-Frontend` 目录执行：

```powershell
npm install
npm run dev
```

默认前端开发地址为 `http://localhost:5173`。API 基础地址由 `VITE_COMPILER_API_BASE_URL` 控制，默认访问同源 `/api/compile/*`。

### 8.3 常见使用流程

1. 启动后端 API。
2. 启动前端工作台。
3. 在源码输入区粘贴或导入 `.snl` 文件。
4. 选择 Token 视图与分析阶段。
5. 点击运行按钮。
6. 查看输出面板中的 Token、错误信息、语法树、符号表或 MIPS 文本。

## 9. 功能边界

### 9.1 已支持

- 词法分析，含字符常量与注释
- LL(1) 语法分析
- 递归下降语法分析
- AST 构建与语法树图展示
- 语义分析与符号表输出
- 部分 MIPS 目标代码生成

### 9.2 仍有限制

- MIPS 后端未完整覆盖全部复杂 SNL 场景
- 错误恢复机制较弱，更多是失败即终止
- `Constants`、`Lexer`、`Parser` 仍保留静态共享状态，不适合高并发服务场景
- 缺少更系统的自动化测试与覆盖率报告

## 10. 扩展方向

- 为当前 API 增加系统化后端测试与前端集成测试
- 降低 `Constants` 静态状态对流程编排的耦合
- 为 MIPS 后端补齐 record、复杂过程调用等场景
- 为语义分析增加更完整的类型系统与错误恢复
- 增加样例管理、结果导出和更细粒度的诊断展示
