// User types
export interface User {
  id: number
  username: string
  email: string
  nickname: string
  avatar?: string
  role: string
  status: string
  createdAt: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  expiresIn: number
  user: User
}

// Assistant types
export interface Assistant {
  id: number
  name: string
  description?: string
  avatar?: string
  systemPrompt?: string
  modelConfig?: string
  capabilities?: string[]
  skills?: Skill[]
  isPublic: boolean
  status: 'draft' | 'pending_review' | 'published' | 'rejected'
  reviewComment?: string
  createdBy?: User
  createdAt: string
  updatedAt: string
}

export interface AssistantCreateRequest {
  name: string
  description?: string
  avatar?: string
  systemPrompt?: string
  modelConfig?: string
  capabilities?: string[]
  skillIds?: number[]
}

// Skill types
export interface Skill {
  id: number
  name: string
  description?: string
  category?: string
  schemaJson: string
  handlerConfig?: string
  isPublic: boolean
  createdAt: string
}

export interface SkillCreateRequest {
  name: string
  description?: string
  category?: string
  schemaJson: string
  handlerConfig?: string
  isPublic?: boolean
}

// Conversation types
export interface Conversation {
  id: number
  userId: number
  assistantId?: number
  title?: string
  metadata?: string
  assistant?: Assistant
  messages?: Message[]
  createdAt: string
  updatedAt: string
}

export interface ConversationCreateRequest {
  assistantId: number
  title?: string
}

// Message types
export interface Message {
  id: number
  conversationId: number
  role: 'user' | 'assistant' | 'system' | 'tool'
  content?: string
  toolCalls?: string
  toolCallId?: string
  thinkingContent?: string
  tokenCount?: number
  metadata?: string
  createdAt: string
}

export interface MessageCreateRequest {
  conversationId: number
  content: string
  metadata?: string
}

// API Response types
export interface ApiResponse<T> {
  success: boolean
  message?: string
  data?: T
  errors?: string[]
}

export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// OpenAI compatible types
export interface ChatCompletionMessage {
  role: 'system' | 'user' | 'assistant' | 'tool'
  content: string
  name?: string
  toolCalls?: ToolCall[]
  toolCallId?: string
}

export interface ToolCall {
  id: string
  type: 'function'
  function: {
    name: string
    arguments: string
  }
}

export interface ChatCompletionRequest {
  model: string
  messages: ChatCompletionMessage[]
  temperature?: number
  topP?: number
  maxTokens?: number
  stream?: boolean
  tools?: ToolDefinition[]
}

export interface ToolDefinition {
  type: 'function'
  function: {
    name: string
    description?: string
    parameters?: Record<string, unknown>
  }
}

export interface ChatCompletionChunk {
  id: string
  object: 'chat.completion.chunk'
  created: number
  model: string
  choices: {
    index: number
    delta: {
      role?: string
      content?: string
      toolCalls?: Partial<ToolCall>[]
    }
    finishReason?: string
  }[]
}