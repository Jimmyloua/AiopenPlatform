import axios from 'axios'
import { useUserStore } from '../stores/userStore'
import {
  ApiResponse,
  AuthResponse,
  User,
  Assistant,
  AssistantCreateRequest,
  Skill,
  SkillCreateRequest,
  Conversation,
  ConversationCreateRequest,
  Message,
  MessageCreateRequest,
  PageResponse,
  ChatCompletionRequest,
  ChatCompletionChunk,
} from '../types'

const API_BASE_URL = '/api'

// Create axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Add auth token to requests
api.interceptors.request.use((config) => {
  const token = useUserStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useUserStore.getState().logout()
    }
    return Promise.reject(error)
  }
)

// Auth API
export const authApi = {
  login: async (username: string, password: string): Promise<ApiResponse<AuthResponse>> => {
    const response = await api.post('/users/login', { username, password })
    return response.data
  },

  register: async (
    username: string,
    email: string,
    password: string,
    nickname?: string
  ): Promise<ApiResponse<AuthResponse>> => {
    const response = await api.post('/users/register', { username, email, password, nickname })
    return response.data
  },

  getCurrentUser: async (): Promise<ApiResponse<User>> => {
    const response = await api.get('/users/me')
    return response.data
  },
}

// Assistant API
export const assistantApi = {
  list: async (page = 1, size = 10, publicOnly = true): Promise<ApiResponse<PageResponse<Assistant>>> => {
    const response = await api.get('/assistants', {
      params: { page, size, publicOnly },
    })
    return response.data
  },

  get: async (id: number): Promise<ApiResponse<Assistant>> => {
    const response = await api.get(`/assistants/${id}`)
    return response.data
  },

  create: async (data: AssistantCreateRequest): Promise<ApiResponse<Assistant>> => {
    const response = await api.post('/assistants', data)
    return response.data
  },

  update: async (id: number, data: AssistantCreateRequest): Promise<ApiResponse<Assistant>> => {
    const response = await api.put(`/assistants/${id}`, data)
    return response.data
  },

  delete: async (id: number): Promise<ApiResponse<void>> => {
    const response = await api.delete(`/assistants/${id}`)
    return response.data
  },

  publish: async (id: number): Promise<ApiResponse<Assistant>> => {
    const response = await api.post(`/assistants/${id}/publish`)
    return response.data
  },

  approve: async (id: number, comment?: string): Promise<ApiResponse<Assistant>> => {
    const response = await api.post(`/assistants/${id}/approve`, null, { params: { comment } })
    return response.data
  },

  reject: async (id: number, comment?: string): Promise<ApiResponse<Assistant>> => {
    const response = await api.post(`/assistants/${id}/reject`, null, { params: { comment } })
    return response.data
  },
}

// Skill API
export const skillApi = {
  list: async (page = 1, size = 10, category?: string): Promise<ApiResponse<PageResponse<Skill>>> => {
    const response = await api.get('/skills', {
      params: { page, size, category },
    })
    return response.data
  },

  get: async (id: number): Promise<ApiResponse<Skill>> => {
    const response = await api.get(`/skills/${id}`)
    return response.data
  },

  create: async (data: SkillCreateRequest): Promise<ApiResponse<Skill>> => {
    const response = await api.post('/skills', data)
    return response.data
  },

  update: async (id: number, data: SkillCreateRequest): Promise<ApiResponse<Skill>> => {
    const response = await api.put(`/skills/${id}`, data)
    return response.data
  },

  delete: async (id: number): Promise<ApiResponse<void>> => {
    const response = await api.delete(`/skills/${id}`)
    return response.data
  },
}

// Conversation API
export const conversationApi = {
  list: async (page = 1, size = 10): Promise<ApiResponse<PageResponse<Conversation>>> => {
    const response = await api.get('/conversations', {
      params: { page, size },
    })
    return response.data
  },

  get: async (id: number): Promise<ApiResponse<Conversation>> => {
    const response = await api.get(`/conversations/${id}`)
    return response.data
  },

  create: async (data: ConversationCreateRequest): Promise<ApiResponse<Conversation>> => {
    const response = await api.post('/conversations', data)
    return response.data
  },

  delete: async (id: number): Promise<ApiResponse<void>> => {
    const response = await api.delete(`/conversations/${id}`)
    return response.data
  },

  getMessages: async (conversationId: number): Promise<ApiResponse<Message[]>> => {
    const response = await api.get(`/conversations/${conversationId}/messages`)
    return response.data
  },

  addMessage: async (conversationId: number, data: MessageCreateRequest): Promise<ApiResponse<Message>> => {
    const response = await api.post(`/conversations/${conversationId}/messages`, data)
    return response.data
  },
}

// OpenAI compatible API
export const openaiApi = {
  chatCompletions: async (request: ChatCompletionRequest): Promise<Response> => {
    const token = useUserStore.getState().token
    const response = await fetch('/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(request),
    })
    return response
  },

  streamChatCompletions: async function* (
    request: ChatCompletionRequest
  ): AsyncGenerator<ChatCompletionChunk> {
    const response = await openaiApi.chatCompletions({ ...request, stream: true })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()

    if (!reader) {
      throw new Error('No reader available')
    }

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value)
      const lines = chunk.split('\n').filter((line) => line.startsWith('data: '))

      for (const line of lines) {
        const data = line.slice(6)
        if (data === '[DONE]') {
          return
        }

        try {
          const parsed = JSON.parse(data) as ChatCompletionChunk
          yield parsed
        } catch {
          // Skip invalid JSON
        }
      }
    }
  },

  listModels: async (): Promise<ApiResponse<{ id: string; object: string }[]>> => {
    const response = await api.get('/v1/models')
    return response.data
  },
}

export default api