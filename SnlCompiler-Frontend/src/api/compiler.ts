export type CompileStage = 'lexical' | 'grammar' | 'recursive' | 'semantic'

export interface CompileRequest {
  source: string
  tokenView: 'external' | 'internal'
}

export interface TokenDto {
  line: number
  type: number
  index: number
  lexeme: string
}

export interface SyntaxTreeNodeDto {
  id: string
  parentId: string | null
  label: string
  kind: string
  line: number
  depth: number
  order: number
}

export interface SyntaxGraphNodeDto {
  id: string
  shape: string
  x: number
  y: number
  width: number
  height: number
  label: string
  kind: string
  line: number
}

export interface SyntaxGraphEdgeDto {
  id: string
  source: string
  target: string
  shape: string
}

export interface SyntaxGraphDto {
  nodes: SyntaxGraphNodeDto[]
  edges: SyntaxGraphEdgeDto[]
}

export interface CompileResponse {
  stage: CompileStage
  success: boolean
  output: string | null
  externalTokenOutput: string | null
  internalTokenOutput: string | null
  grammarOutput: string | null
  astOutput: string | null
  symbolTableOutput: string | null
  errors: string[]
  tokens: TokenDto[]
  syntaxTree: SyntaxTreeNodeDto[]
  syntaxGraph: SyntaxGraphDto
}

const API_BASE_URL = import.meta.env.VITE_COMPILER_API_BASE_URL ?? ''

export async function compileStage(
  stage: CompileStage,
  request: CompileRequest,
): Promise<CompileResponse> {
  const response = await fetch(`${API_BASE_URL}/api/compile/${stage}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    throw new Error(`API request failed with HTTP ${response.status}`)
  }

  return response.json()
}
