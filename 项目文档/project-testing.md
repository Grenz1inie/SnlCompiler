# SNL Compiler 项目测试文档

## 1. 文档目标

本文档按当前仓库实际结构整理测试建议，目标是让读者能够围绕现有实现验证以下能力：

- 词法分析
- LL(1) 语法分析
- 递归下降语法分析
- 语义分析
- MIPS 目标代码生成
- 前后端联调闭环

当前仓库中已经存在后端测试入口 `SnlCompiler-Backend/src/test/java/com/snl/compiler/CompilerApplicationTests.java`，但缺少覆盖核心业务逻辑的系统化自动化测试。本文因此以“可执行手工验证 + 可补充临时自动化样例”为主，而不再引用不存在的 `tools/FullTest.java` 或旧版 `bin + javac` 工作流。

## 2. 测试环境

| 项目 | 要求 |
| --- | --- |
| JDK | 21 |
| Node.js | 20 或更高版本 |
| 后端构建 | Maven Wrapper（`./mvnw`） |
| 前端构建 | npm + Vite |
| 编码 | UTF-8 |

## 3. 启动方式

### 3.1 后端

在 [SnlCompiler-Backend](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Backend) 目录执行：

```powershell
./mvnw test
./mvnw spring-boot:run
```

默认后端会启动在本地 Spring Boot 服务端口，API 入口为 `/api/compile/*`。

### 3.2 前端

在 [SnlCompiler-Frontend](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Frontend) 目录执行：

```powershell
npm install
npm run dev
```

启动后通过浏览器访问 Vite 开发地址进行手工验证。

## 4. 被测对象

### 4.1 后端核心类

- [Lexer.java](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Backend/src/main/java/com/snl/compiler/core/lexer/Lexer.java)
- [Parser.java](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Backend/src/main/java/com/snl/compiler/core/parser/Parser.java)
- [RecursiveDescentParser.java](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Backend/src/main/java/com/snl/compiler/core/parser/RecursiveDescentParser.java)
- [SemanticAnalyzer.java](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Backend/src/main/java/com/snl/compiler/core/semantic/SemanticAnalyzer.java)
- [MipsCodeGenerator.java](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Backend/src/main/java/com/snl/compiler/application/codegen/MipsCodeGenerator.java)
- [CompilerPipeline.java](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Backend/src/main/java/com/snl/compiler/application/CompilerPipeline.java)
- [Constants.java](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Backend/src/main/java/com/snl/compiler/infrastructure/config/Constants.java)

### 4.2 前端核心文件

- [compiler.ts](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Frontend/src/api/compiler.ts)
- [CompilerWorkbench.vue](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Frontend/src/components/CompilerWorkbench.vue)

### 4.3 示例程序

- [sample.snl](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Backend/src/main/resources/samples/sample.snl)
- [comment_test.snl](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Backend/src/main/resources/samples/comment_test.snl)
- [char_test.snl](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Backend/src/main/resources/samples/char_test.snl)
- [record_test.snl](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Backend/src/main/resources/samples/record_test.snl)
- [bubble_sort.snl](E:/Materials/编译原理/上机实验/SnlCompiler/SnlCompiler-Backend/src/main/resources/samples/bubble_sort.snl)

## 5. 测试分层策略

| 层级 | 目标 | 说明 |
| --- | --- | --- |
| 单元级 | 验证识别函数、分析入口、核心约束 | 适合围绕 `Lexer`、`Parser`、`SemanticAnalyzer` 编写新增测试 |
| 接口级 | 验证 `/api/compile/*` 的请求与响应结构 | 重点检查 `success`、`output`、`errors`、`tokens`、`syntaxGraph` |
| 端到端 | 验证前端工作台的用户操作闭环 | 包括阶段切换、结果展示、语法树绘制、错误提示 |

## 6. 推荐验证矩阵

| 类别 | 场景 | 样例 | 预期 |
| --- | --- | --- | --- |
| 词法成功 | 基础程序 | `sample.snl` | 返回 `success=true`，出现 Token 输出 |
| 词法成功 | 注释跳过 | `comment_test.snl` | 注释不进入 Token 序列 |
| 词法成功 | 字符常量 | `char_test.snl` | 生成类型 5 Token |
| 词法异常 | 非法字符 | 手输 `program p#` | 返回失败并提示“无法识别” |
| 词法异常 | 缺少句点 | 手输缺少 `.` 的程序 | 返回失败并提示“程序未能正常结束” |
| LL(1) 成功 | 基础程序 | `sample.snl` | `grammarOutput` 含“语法分析成功” |
| LL(1) 成功 | 冒泡排序 | `bubble_sort.snl` | 可输出 LL(1) 成功信息 |
| 递归下降成功 | record | `record_test.snl` | AST 构建成功，`syntaxGraph.nodes` 非空 |
| 语义成功 | 基础程序 | `sample.snl` | 返回符号表，错误列表为空 |
| 语义异常 | 未声明变量 | 自定义样例 | 报未声明错误 |
| 语义异常 | 重复定义 | 自定义样例 | 报重复定义错误 |
| 代码生成 | 简单读写程序 | `sample.snl` | 返回 MIPS 文本 |
| 代码生成限制 | record 或复杂结构 | `record_test.snl` 等 | 允许出现“暂不支持”类提示 |

## 7. API 验证建议

后端接口统一为：

- `POST /api/compile/lexical`
- `POST /api/compile/grammar`
- `POST /api/compile/recursive`
- `POST /api/compile/semantic`
- `POST /api/compile/codegen`

请求体：

```json
{
  "source": "program demo\nbegin\nend.",
  "tokenView": "external"
}
```

通用响应关键字段：

- `stage`
- `success`
- `output`
- `externalTokenOutput`
- `internalTokenOutput`
- `errors`
- `tokens`
- `syntaxTree`
- `syntaxGraph`
- `symbolTableOutput`
- `mipsOutput`

接口级测试应重点确认：

- 各阶段 `stage` 字段正确
- 词法失败时 `success=false` 且 `errors` 包含输出信息
- 语法阶段会同时返回 Token 与语法树图数据
- 语义阶段在 AST 缺失时会终止并给出明确提示
- 代码生成阶段在语义失败时不会继续输出有效 MIPS

## 8. 前端手工测试

当前前端不是旧版“多个独立按钮 + 帮助弹窗 + 状态栏”的形态，而是统一阶段切换工作台。手工测试应按当前 UI 验证：

1. 打开工作台，确认源码输入区、阶段切换器、运行按钮可见。
2. 点击“示例”，确认样例源码被填入。
3. 切换 `外部表示 / 内部表示`，运行 `词法分析`，确认输出变化。
4. 切换到 `LL(1)`，运行并检查结果面板。
5. 切换到 `RD`，确认语法树区域绘制节点。
6. 切换到 `SEM`，确认输出含符号表。
7. 切换到 `MIPS`，确认输出含 MIPS 文本或限制提示。
8. 使用“导入”加载 `.snl` 文件，确认文件内容进入编辑区。
9. 输入非法程序，确认错误信息在结果区展示。

## 9. 当前测试缺口

按当前仓库实际情况，以下内容仍应视为待补强项：

- 缺少覆盖 `CompilerPipeline` 五阶段行为的后端测试
- 缺少针对 `/api/compile/*` 的控制器或集成测试
- 缺少前端组件测试或端到端测试脚本
- 缺少覆盖率统计工具与报告
- 缺少对 MIPS 后端限制场景的系统化回归样例

## 10. 回归建议

每次修改核心逻辑后，建议最少执行：

1. 后端 `./mvnw test`
2. 前端 `npm run build`
3. 手工验证 `sample.snl` 的词法、LL(1)、递归下降、语义、MIPS 五个阶段
4. 手工验证一个词法错误样例
5. 手工验证一个语义错误样例
6. 手工验证一个代码生成受限样例，确认提示信息仍合理

## 11. 结论

这份测试文档只陈述当前仓库可证实的事实，不再引用旧版 `Main` 启动方式、`tools/FullTest.java`、`src/TestData.txt` 或基于 `javac -d bin` 的历史流程。若后续补入正式自动化测试，应优先围绕 `CompilerPipeline` 和 `/api/compile/*` 建立稳定回归基线。
