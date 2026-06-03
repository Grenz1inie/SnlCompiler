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
