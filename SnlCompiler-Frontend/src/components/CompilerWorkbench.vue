<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElButton, ElRadioButton, ElRadioGroup, ElSegmented, ElTag, ElTooltip } from 'element-plus'
import { Graph } from '@antv/x6'
import {
  compileStage,
  type CompileResponse,
  type CompileStage,
  type SyntaxGraphNodeDto,
  type TokenDto,
} from '@/api/compiler'

interface StageAction {
  id: CompileStage
  label: string
  shortLabel: string
  hint: string
}

interface LexemeTableEntry {
  index: number
  lexeme: string
  count: number
  lines: number[]
}

interface LexemeTable {
  type: number
  title: string
  entries: LexemeTableEntry[]
}

const stages: StageAction[] = [
  { id: 'lexical', label: '词法分析', shortLabel: 'Lex', hint: '生成 token 流和两种 token 表示' },
  {
    id: 'grammar',
    label: 'LL(1)语法分析',
    shortLabel: 'LL(1)',
    hint: '绘制普通语法树并报告语法分析结果',
  },
  {
    id: 'recursive',
    label: '递归下降分析',
    shortLabel: 'RD',
    hint: '绘制普通语法树并报告语法分析结果',
  },
  { id: 'semantic', label: '语义分析', shortLabel: 'SEM', hint: '执行语义检查并输出符号表' },
  {
    id: 'codegen',
    label: 'MIPS 代码生成',
    shortLabel: 'MIPS',
    hint: '语义通过后生成 MIPS 汇编，可复制到 MARS/QtSpim 运行',
  },
]

const stageOptions = stages.map((stage) => ({
  label: stage.shortLabel,
  value: stage.id,
}))

const sampleSource = `program demo
var integer x;
begin
  read(x);
  write(x)
end.`

const source = ref(sampleSource)
const tokenView = ref<'external' | 'internal'>('external')
const activeStage = ref<CompileStage>('lexical')
const result = ref<CompileResponse | null>(null)
const loadingStage = ref<CompileStage | null>(null)
const errorMessage = ref('')
const syntaxGraphContainer = ref<HTMLDivElement | null>(null)
const sourceEditor = ref<HTMLTextAreaElement | null>(null)
const lineGutter = ref<HTMLDivElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
let syntaxGraph: Graph | null = null
let syntaxGraphElement: HTMLDivElement | null = null

const canRun = computed(() => source.value.trim().length > 0 && loadingStage.value === null)
const activeStageMeta = computed(() => {
  const stage = stages.find((s) => s.id === activeStage.value)
  return stage!
})
const sourceLineCount = computed(() => source.value.split(/\r\n|\r|\n/).length)
const gutterColumnWidth = computed(() => {
  const digits = String(sourceLineCount.value).length
  return `${Math.max(2, digits) + 1}ch`
})
const tokenCount = computed(() => result.value?.tokens.length ?? 0)
const errorCount = computed(() => result.value?.errors.length ?? 0)
const hasSyntaxGraph = computed(() => (result.value?.syntaxGraph?.nodes.length ?? 0) > 0)

const resultTitle = computed(() => {
  if (!result.value) {
    return '等待分析'
  }
  const stage = stages.find((item) => item.id === result.value?.stage)
  return stage?.label ?? '分析结果'
})

const displayOutput = computed(() => {
  if (errorMessage.value) {
    return errorMessage.value
  }
  if (!result.value) {
    return '输入 SNL 源码后选择一个分析阶段。'
  }
  return result.value.output || '没有输出。'
})

const selectedTokenOutput = computed(() => {
  if (!result.value) {
    return ''
  }
  if (tokenView.value === 'internal') {
    return result.value.internalTokenOutput || ''
  }
  return result.value.externalTokenOutput || ''
})

function describeTokenType(type: number) {
  switch (type) {
    case 1:
      return '分隔符 / 符号'
    case 2:
      return '保留字'
    case 3:
      return '标识符'
    case 4:
      return '数字常量'
    case 5:
      return '字符常量'
    default:
      return '未知'
  }
}

const tokenTableTypes = [1, 2, 3, 4, 5]

const lexemeTables = computed<LexemeTable[]>(() => {
  const tokens = result.value?.tokens ?? []
  return tokenTableTypes
    .map((type) => ({
      type,
      title: `${describeTokenType(type)}表`,
      entries: buildLexemeTableEntries(tokens, type),
    }))
    .filter((table) => table.entries.length > 0)
})

function buildLexemeTableEntries(tokens: TokenDto[], type: number) {
  const entries = new Map<number, LexemeTableEntry>()

  tokens
    .filter((token) => token.type === type)
    .forEach((token) => {
      const entry = entries.get(token.index)
      if (entry) {
        entry.count += 1
        if (!entry.lines.includes(token.line)) {
          entry.lines.push(token.line)
        }
        return
      }

      entries.set(token.index, {
        index: token.index,
        lexeme: token.lexeme,
        count: 1,
        lines: [token.line],
      })
    })

  return Array.from(entries.values()).sort((left, right) => left.index - right.index)
}

const resultOutputTitle = computed(() => {
  if (result.value?.stage === 'semantic') {
    return '语义分析结果'
  }
  if (result.value?.stage === 'codegen') {
    return 'MIPS 目标代码'
  }
  if (result.value?.stage === 'lexical') {
    return tokenView.value === 'internal' ? '内部表示' : '外部表示'
  }
  return '语法分析结果'
})

const combinedOutput = computed(() => {
  if (errorMessage.value || !result.value) {
    return displayOutput.value
  }
  if (result.value.stage !== 'grammar' && result.value.stage !== 'recursive') {
    return displayOutput.value
  }

  const tokenOutput = selectedTokenOutput.value.trim()
  const syntaxOutput = displayOutput.value.trim()
  if (!tokenOutput) {
    return syntaxOutput
  }
  return `${tokenOutput}\n\n--- 语法分析结果 ---\n${syntaxOutput}`
})

function registerSyntaxShapes() {
  Graph.registerNode(
    'syntax-node',
    {
      inherit: 'rect',
      width: 170,
      height: 58,
      attrs: {
        body: {
          rx: 8,
          ry: 8,
          fill: '#0f172a',
          stroke: '#5eead4',
          strokeWidth: 1.5,
          filter: 'drop-shadow(0 12px 22px rgba(20, 184, 166, 0.22))',
        },
        label: {
          fill: '#e0f2fe',
          fontFamily: 'Cascadia Code, JetBrains Mono, Consolas, monospace',
          fontSize: 12,
          fontWeight: 700,
          textWrap: {
            width: 148,
            height: 44,
            ellipsis: true,
          },
        },
      },
    },
    true,
  )

  Graph.registerEdge(
    'syntax-edge',
    {
      inherit: 'edge',
      zIndex: 0,
      attrs: {
        line: {
          stroke: '#38bdf8',
          strokeWidth: 2.3,
          strokeDasharray: '9 7',
          targetMarker: {
            name: 'block',
            width: 9,
            height: 7,
          },
          class: 'syntax-edge-line',
        },
      },
    },
    true,
  )
}

function createSyntaxGraph() {
  if (!syntaxGraphContainer.value) {
    return
  }

  syntaxGraph?.dispose()
  syntaxGraphElement = syntaxGraphContainer.value
  syntaxGraph = new Graph({
    container: syntaxGraphContainer.value,
    autoResize: true,
    panning: true,
    mousewheel: {
      enabled: true,
      modifiers: ['ctrl', 'meta'],
      minScale: 0.25,
      maxScale: 1.7,
    },
    interacting: {
      nodeMovable: true,
    },
    background: {
      color: 'transparent',
    },
    grid: {
      visible: true,
      type: 'dot',
      size: 18,
      args: {
        color: 'rgba(148, 163, 184, 0.28)',
        thickness: 1,
      },
    },
  })
}

function nodeStroke(kind: string) {
  const normalized = kind.toLowerCase()
  if (normalized === 'prok' || normalized === 'pheadk') {
    return '#fbbf24'
  }
  if (normalized === 'stmk' || normalized === 'expk') {
    return '#60a5fa'
  }
  return '#5eead4'
}

function renderSyntaxGraph() {
  const graph = result.value?.syntaxGraph
  if (!graph || graph.nodes.length === 0) {
    syntaxGraph?.dispose()
    syntaxGraph = null
    syntaxGraphElement = null
    return
  }

  if (!syntaxGraph || syntaxGraphElement !== syntaxGraphContainer.value) {
    createSyntaxGraph()
  }
  if (!syntaxGraph) {
    return
  }

  syntaxGraph.clearCells()

  const nodeMetas = graph.nodes.map((node: SyntaxGraphNodeDto) => ({
    id: node.id,
    shape: node.shape || 'syntax-node',
    x: node.x,
    y: node.y,
    width: node.width,
    height: node.height,
    attrs: {
      body: {
        stroke: nodeStroke(node.kind),
      },
      label: {
        text: node.label,
      },
    },
    data: {
      kind: node.kind,
      line: node.line,
    },
  }))

  const edgeMetas = graph.edges.map((edge) => ({
    id: edge.id,
    shape: edge.shape || 'syntax-edge',
    source: { cell: edge.source },
    target: { cell: edge.target },
    connector: { name: 'rounded' },
    router: { name: 'normal' },
  }))

  nodeMetas.forEach((meta) => syntaxGraph!.addNode(meta))
  edgeMetas.forEach((meta) => syntaxGraph!.addEdge(meta))

  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      if (!syntaxGraph) {
        return
      }
      syntaxGraph.centerContent()
      syntaxGraph.zoomToFit({
        padding: 40,
        maxScale: 1,
      })
    })
  })
}

async function runStage(stage: CompileStage = activeStage.value) {
  if (!canRun.value) {
    return
  }

  activeStage.value = stage
  loadingStage.value = stage
  errorMessage.value = ''

  try {
    result.value = await compileStage(stage, {
      source: source.value,
      tokenView: tokenView.value,
    })
  } catch (error) {
    const message = error instanceof Error ? error.message : '无法连接后端 API。'
    errorMessage.value = `${message}\n请确认后端 CompilerHttpServer 已在 http://localhost:8080 启动。`
  } finally {
    loadingStage.value = null
  }
}

function resetSource() {
  source.value = sampleSource
  result.value = null
  errorMessage.value = ''
  syntaxGraph?.clearCells()
}

function clearSource() {
  source.value = ''
  result.value = null
  errorMessage.value = ''
  syntaxGraph?.clearCells()
}

function triggerImport() {
  fileInputRef.value?.click()
}

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }

  try {
    source.value = await file.text()
    result.value = null
    errorMessage.value = ''
    syntaxGraph?.clearCells()
  } catch {
    errorMessage.value = '无法读取文件，请确认文件为 UTF-8 编码的文本。'
  }
}

function syncEditorScroll() {
  if (lineGutter.value && sourceEditor.value) {
    lineGutter.value.scrollTop = sourceEditor.value.scrollTop
  }
}

onMounted(() => {
  registerSyntaxShapes()
  createSyntaxGraph()
})

onBeforeUnmount(() => {
  syntaxGraph?.dispose()
  syntaxGraph = null
  syntaxGraphElement = null
})

watch(
  () => [result.value?.stage, result.value?.syntaxGraph?.nodes.length ?? 0],
  async () => {
    await nextTick()
    requestAnimationFrame(renderSyntaxGraph)
  },
)
</script>

<template>
  <main class="compiler-shell">
    <div class="aurora aurora-one" />
    <div class="aurora aurora-two" />

    <section class="hero-band">
      <div class="hero-copy">
        <p class="eyebrow">SNL Compiler</p>
        <h1>编译分析工作台</h1>
        <p class="subtitle">
          后端负责编译核心逻辑的处理，前端通过 API 承接词法、语法、递归下降和语义分析。
        </p>
      </div>

      <div class="hero-metrics">
        <div class="metric-tile">
          <span>{{ sourceLineCount }}</span>
          <small>源码行</small>
        </div>
        <div class="metric-tile">
          <span>{{ tokenCount }}</span>
          <small>Token</small>
        </div>
        <div class="metric-tile">
          <span>{{ errorCount }}</span>
          <small>错误</small>
        </div>
      </div>
    </section>

    <section class="control-strip">
      <div class="stage-picker">
        <el-segmented v-model="activeStage" :options="stageOptions" size="large" />
        <span class="stage-hint">{{ activeStageMeta.hint }}</span>
      </div>

      <div class="control-actions">
        <el-radio-group v-model="tokenView" size="large">
          <el-radio-button value="external">外部表示</el-radio-button>
          <el-radio-button value="internal">内部表示</el-radio-button>
        </el-radio-group>

        <el-tooltip content="调用当前选中的分析阶段" placement="bottom">
          <el-button
            class="run-button"
            type="primary"
            size="large"
            :loading="loadingStage !== null"
            :disabled="!canRun"
            @click="runStage()"
          >
            运行 {{ activeStageMeta.label }}
          </el-button>
        </el-tooltip>
      </div>
    </section>

    <section class="workspace">
      <div class="glass-panel editor-pane">
        <div class="pane-head">
          <div>
            <h2>源码输入</h2>
            <p>API: /api/compile</p>
          </div>

          <div class="editor-actions">
            <input
              ref="fileInputRef"
              type="file"
              class="file-input-hidden"
              accept=".snl,.txt,text/plain"
              @change="onFileSelected"
            />
            <el-button size="small" @click="triggerImport">导入</el-button>
            <el-button size="small" @click="resetSource">示例</el-button>
            <el-button size="small" @click="clearSource">清空</el-button>
          </div>
        </div>

        <div class="editor-frame" :style="{ gridTemplateColumns: `${gutterColumnWidth} 1fr` }">
          <div ref="lineGutter" class="line-gutter" aria-hidden="true">
            <span v-for="line in sourceLineCount" :key="line">{{ line }}</span>
          </div>
          <textarea
            ref="sourceEditor"
            v-model="source"
            class="source-editor"
            spellcheck="false"
            aria-label="SNL 源码输入"
            @scroll="syncEditorScroll"
          />
        </div>
      </div>

      <div class="glass-panel result-pane">
        <div class="pane-head">
          <div>
            <h2>{{ resultTitle }}</h2>
            <p>{{ result ? activeStageMeta.hint : '选择阶段后运行分析' }}</p>
          </div>

          <el-tag
            :type="result ? (result.success ? 'success' : 'warning') : 'info'"
            effect="dark"
            round
          >
            {{ result ? (result.success ? '成功' : '待检查') : '未运行' }}
          </el-tag>
        </div>

        <div class="analysis-output">
          <div v-if="hasSyntaxGraph" class="tree-section">
            <div class="section-caption">
              <span>AntV X6 Syntax Tree</span>
              <small>{{ result?.syntaxGraph?.nodes.length ?? 0 }} nodes</small>
            </div>
            <div ref="syntaxGraphContainer" class="tree-canvas" />
          </div>

          <div class="output-section">
            <div class="section-caption">
              <span>{{ resultOutputTitle }}</span>
            </div>
            <pre class="output-view result-output">{{ combinedOutput }}</pre>
          </div>
        </div>

        <Transition name="slide-up">
          <div v-if="lexemeTables.length" class="lexeme-tables-wrap">
            <div class="lexeme-table-grid">
              <section v-for="table in lexemeTables" :key="table.type" class="lexeme-table-card">
                <div class="lexeme-table-head">
                  <span>{{ table.title }}</span>
                  <small>type={{ table.type }}</small>
                </div>
                <table class="token-table">
                  <thead>
                    <tr>
                      <th>下标</th>
                      <th>词素</th>
                      <th>次数</th>
                      <th>行号</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="entry in table.entries" :key="`${table.type}-${entry.index}`">
                      <td>{{ entry.index }}</td>
                      <td>{{ entry.lexeme }}</td>
                      <td>{{ entry.count }}</td>
                      <td>{{ entry.lines.join(', ') }}</td>
                    </tr>
                  </tbody>
                </table>
              </section>
            </div>
          </div>
        </Transition>
      </div>
    </section>
  </main>
</template>

<style scoped>
.compiler-shell {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  color: #e5edf7;
  background:
    radial-gradient(circle at 18% 16%, rgba(20, 184, 166, 0.22), transparent 32%),
    radial-gradient(circle at 86% 12%, rgba(99, 102, 241, 0.24), transparent 36%),
    linear-gradient(135deg, #08111f 0%, #111827 42%, #152238 100%);
}

.aurora {
  position: absolute;
  width: 360px;
  height: 360px;
  border-radius: 999px;
  filter: blur(36px);
  opacity: 0.42;
  pointer-events: none;
  animation: drift 12s ease-in-out infinite alternate;
}

.aurora-one {
  top: 92px;
  left: -130px;
  background: #14b8a6;
}

.aurora-two {
  right: -120px;
  bottom: 80px;
  background: #6366f1;
  animation-delay: -4s;
}

.hero-band,
.control-strip,
.workspace {
  position: relative;
  z-index: 1;
}

.hero-band {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
  padding: 30px 32px 18px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #5eead4;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  color: #f8fafc;
  font-size: 34px;
  font-weight: 800;
}

h2 {
  color: #f8fafc;
  font-size: 16px;
  font-weight: 750;
}

.subtitle {
  max-width: 720px;
  margin-top: 8px;
  color: #b9c7d8;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, 92px);
  gap: 10px;
}

.metric-tile {
  min-height: 78px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  padding: 12px;
  background: rgba(15, 23, 42, 0.58);
  box-shadow: 0 16px 38px rgba(0, 0, 0, 0.2);
}

.metric-tile span {
  display: block;
  color: #ffffff;
  font-size: 24px;
  font-weight: 800;
}

.metric-tile small {
  color: #9fb0c5;
  font-size: 12px;
}

.control-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 0 32px 16px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 8px;
  padding: 12px;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(16px);
}

.stage-picker,
.control-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stage-hint {
  color: #b9c7d8;
  font-size: 13px;
}

.run-button {
  min-width: 168px;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.35);
}

.workspace {
  display: grid;
  grid-template-columns: minmax(360px, 0.95fr) minmax(420px, 1.05fr);
  gap: 16px;
  padding: 0 32px 32px;
}

.glass-panel {
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.72);
  box-shadow: 0 24px 80px rgba(2, 6, 23, 0.38);
  backdrop-filter: blur(18px);
}

.pane-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 74px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.pane-head p {
  margin-top: 3px;
  color: #9fb0c5;
  font-size: 13px;
}

.editor-actions {
  display: flex;
  gap: 8px;
}

.file-input-hidden {
  display: none;
}

.editor-frame {
  display: grid;
  height: calc(100vh - 292px);
  min-height: 420px;
  overflow: hidden;
}

.line-gutter {
  overflow: hidden;
  padding: 16px 10px 16px 6px;
  border-right: 1px solid rgba(94, 234, 212, 0.28);
  color: #64748b;
  background: rgba(2, 6, 23, 0.52);
  font-family: 'Cascadia Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 14px;
  line-height: 1.55;
  font-variant-numeric: tabular-nums;
  text-align: right;
  user-select: none;
}

.line-gutter span {
  display: block;
  min-height: calc(14px * 1.55);
}

.source-editor {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
  resize: none;
  overflow: auto;
  padding: 16px;
  color: #dbeafe;
  caret-color: #5eead4;
  background: rgba(8, 13, 25, 0.76);
  font-family: 'Cascadia Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 14px;
  line-height: 1.55;
  outline: none;
  box-sizing: border-box;
}

.source-editor:focus {
  box-shadow: inset 0 0 0 1px rgba(94, 234, 212, 0.38);
}

.analysis-output {
  display: grid;
  grid-template-rows: auto auto;
}

.tree-section,
.output-section {
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.section-caption {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 38px;
  padding: 8px 14px;
  color: #93c5fd;
  background: rgba(2, 6, 23, 0.35);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
}

.section-caption small {
  color: #9fb0c5;
  font-size: 12px;
  font-weight: 700;
  text-transform: none;
}

.tree-canvas {
  position: relative;
  height: 42vh;
  min-height: 330px;
  max-height: 520px;
  overflow: hidden;
  background:
    linear-gradient(rgba(148, 163, 184, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.05) 1px, transparent 1px),
    radial-gradient(circle at 20% 20%, rgba(20, 184, 166, 0.16), transparent 28%),
    radial-gradient(circle at 80% 20%, rgba(59, 130, 246, 0.18), transparent 30%),
    rgba(7, 12, 24, 0.72);
  background-size:
    32px 32px,
    32px 32px,
    auto,
    auto,
    auto;
}

:deep(.x6-node rect) {
  transition:
    stroke 0.2s ease,
    filter 0.2s ease,
    transform 0.2s ease;
}

:deep(.x6-node:hover rect) {
  filter: drop-shadow(0 0 16px rgba(94, 234, 212, 0.42));
}

:deep(.syntax-edge-line) {
  filter: drop-shadow(0 0 6px rgba(56, 189, 248, 0.45));
}

.output-view {
  margin: 0;
  overflow: auto;
  padding: 16px;
  background: linear-gradient(rgba(20, 184, 166, 0.05) 1px, transparent 1px), rgba(7, 12, 24, 0.78);
  background-size: 100% 28px;
  font-family: 'Cascadia Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
  line-height: 1.55;
  white-space: pre-wrap;
}

.result-output {
  max-height: 300px;
  min-height: 180px;
  color: #dcfce7;
}

.lexeme-tables-wrap {
  max-height: 32vh;
  overflow: auto;
  border-top: 1px solid rgba(148, 163, 184, 0.2);
  padding: 12px;
}

.lexeme-table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.lexeme-table-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(7, 12, 24, 0.42);
}

.lexeme-table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 9px 10px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  color: #bfdbfe;
  font-size: 13px;
  font-weight: 800;
}

.lexeme-table-head small {
  flex: 0 0 auto;
  color: #5eead4;
  font-family: 'Cascadia Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 11px;
  font-weight: 750;
}

.token-table {
  width: 100%;
  border-collapse: collapse;
  color: #dbeafe;
  font-size: 13px;
}

.token-table th,
.token-table td {
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
  padding: 8px 10px;
  text-align: left;
}

.token-table th {
  position: sticky;
  top: 0;
  color: #93c5fd;
  background: rgba(15, 23, 42, 0.96);
  font-weight: 750;
}

.token-table tr {
  transition:
    background 0.18s ease,
    transform 0.18s ease;
}

.token-table tbody tr:hover {
  background: rgba(37, 99, 235, 0.18);
}

.panel-fade-enter-active,
.panel-fade-leave-active,
.slide-up-enter-active,
.slide-up-leave-active {
  transition:
    opacity 0.24s ease,
    transform 0.24s ease;
}

.panel-fade-enter-from,
.panel-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(18px);
}

:deep(.el-segmented) {
  --el-segmented-bg-color: rgba(15, 23, 42, 0.88);
  --el-segmented-item-selected-bg-color: #2563eb;
  --el-segmented-item-selected-color: #ffffff;
  --el-segmented-item-hover-bg-color: rgba(37, 99, 235, 0.22);
  --el-border-radius-base: 6px;
}

:deep(.el-radio-button__inner) {
  background: rgba(15, 23, 42, 0.72);
  border-color: rgba(148, 163, 184, 0.34);
  color: #dbeafe;
}

:deep(.el-button) {
  border-radius: 6px;
  font-weight: 700;
}

@keyframes drift {
  from {
    transform: translate3d(0, 0, 0) scale(1);
  }
  to {
    transform: translate3d(42px, -28px, 0) scale(1.12);
  }
}

@media (max-width: 1040px) {
  .hero-band,
  .control-strip {
    align-items: stretch;
    flex-direction: column;
  }

  .hero-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .stage-picker,
  .control-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .workspace {
    grid-template-columns: 1fr;
  }

  .editor-frame {
    height: 380px;
    min-height: 380px;
  }
}

@media (max-width: 640px) {
  .hero-band {
    padding: 22px 16px 14px;
  }

  .control-strip {
    margin: 0 16px 14px;
  }

  .workspace {
    padding: 0 16px 20px;
  }

  h1 {
    font-size: 28px;
  }
}
</style>
