<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElButton, ElRadioButton, ElRadioGroup, ElSegmented, ElTag, ElTooltip } from 'element-plus'
import { compileStage, type CompileResponse, type CompileStage } from '@/api/compiler'

interface StageAction {
  id: CompileStage
  label: string
  shortLabel: string
  hint: string
}

const stages: StageAction[] = [
  { id: 'lexical', label: '词法分析', shortLabel: 'Lex', hint: '生成 token 流和两种 token 表示' },
  {
    id: 'grammar',
    label: 'LL(1)语法分析',
    shortLabel: 'LL(1)',
    hint: '执行预测分析并输出规约过程',
  },
  {
    id: 'recursive',
    label: '递归下降分析',
    shortLabel: 'RD',
    hint: '构建抽象语法树并返回解析错误',
  },
  { id: 'semantic', label: '语义分析', shortLabel: 'SEM', hint: '执行语义检查并输出符号表' },
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

const canRun = computed(() => source.value.trim().length > 0 && loadingStage.value === null)
const activeStageMeta = computed(() => {
  const stage = stages.find((s) => s.id === activeStage.value)
  return stage! // 或者是 stages[0] as StageAction
})
const sourceLineCount = computed(() => source.value.split(/\r\n|\r|\n/).length)
const tokenCount = computed(() => result.value?.tokens.length ?? 0)
const errorCount = computed(() => result.value?.errors.length ?? 0)

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
}

function clearSource() {
  source.value = ''
  result.value = null
  errorMessage.value = ''
}
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
          后端编译核心保持不变，前端通过 API 承接词法、语法、递归下降和语义分析。
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
          <el-radio-button label="external">外部表示</el-radio-button>
          <el-radio-button label="internal">内部表示</el-radio-button>
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
            <el-button size="small" @click="resetSource">示例</el-button>
            <el-button size="small" @click="clearSource">清空</el-button>
          </div>
        </div>

        <div class="editor-frame">
          <div class="line-gutter" aria-hidden="true">
            <span v-for="line in sourceLineCount" :key="line">{{ line }}</span>
          </div>
          <textarea
            v-model="source"
            class="source-editor"
            spellcheck="false"
            aria-label="SNL 源码输入"
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

        <Transition name="panel-fade" mode="out-in">
          <pre :key="displayOutput" class="output-view">{{ displayOutput }}</pre>
        </Transition>

        <Transition name="slide-up">
          <div v-if="result?.tokens.length" class="token-table-wrap">
            <table class="token-table">
              <thead>
                <tr>
                  <th>行</th>
                  <th>类型</th>
                  <th>索引</th>
                  <th>词素</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(token, index) in result.tokens" :key="`${token.line}-${index}`">
                  <td>{{ token.line }}</td>
                  <td>{{ token.type }}</td>
                  <td>{{ token.index }}</td>
                  <td>{{ token.lexeme }}</td>
                </tr>
              </tbody>
            </table>
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

.editor-frame {
  display: grid;
  grid-template-columns: 48px 1fr;
  height: calc(100vh - 292px);
  min-height: 420px;
}

.line-gutter {
  overflow: hidden;
  padding: 16px 10px;
  color: #64748b;
  background: rgba(2, 6, 23, 0.52);
  font-family: 'Cascadia Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
  line-height: 1.55;
  text-align: right;
  user-select: none;
}

.line-gutter span {
  display: block;
}

.source-editor {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
  resize: none;
  padding: 16px;
  color: #dbeafe;
  caret-color: #5eead4;
  background: rgba(8, 13, 25, 0.76);
  font-family: 'Cascadia Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 14px;
  line-height: 1.55;
  outline: none;
}

.source-editor:focus {
  box-shadow: inset 0 0 0 1px rgba(94, 234, 212, 0.38);
}

.output-view {
  height: 44vh;
  min-height: 260px;
  margin: 0;
  overflow: auto;
  padding: 16px;
  color: #dcfce7;
  background: linear-gradient(rgba(20, 184, 166, 0.05) 1px, transparent 1px), rgba(7, 12, 24, 0.78);
  background-size: 100% 28px;
  font-family: 'Cascadia Code', 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
  line-height: 1.55;
  white-space: pre-wrap;
}

.token-table-wrap {
  max-height: 32vh;
  overflow: auto;
  border-top: 1px solid rgba(148, 163, 184, 0.2);
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
