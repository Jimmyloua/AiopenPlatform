import { create } from 'zustand'
import { Assistant, PageResponse } from '../types'

interface AssistantState {
  assistants: Assistant[]
  currentAssistant: Assistant | null
  totalPages: number
  currentPage: number
  isLoading: boolean
  setAssistants: (assistants: Assistant[]) => void
  setCurrentAssistant: (assistant: Assistant | null) => void
  setPageInfo: (page: PageResponse<Assistant>) => void
  addAssistant: (assistant: Assistant) => void
  updateAssistant: (id: number, assistant: Assistant) => void
  removeAssistant: (id: number) => void
}

export const useAssistantStore = create<AssistantState>((set) => ({
  assistants: [],
  currentAssistant: null,
  totalPages: 0,
  currentPage: 1,
  isLoading: false,

  setAssistants: (assistants) => set({ assistants }),

  setCurrentAssistant: (assistant) => set({ currentAssistant: assistant }),

  setPageInfo: (page) => set({
    assistants: page.records,
    totalPages: page.pages,
    currentPage: page.current
  }),

  addAssistant: (assistant) => set((state) => ({
    assistants: [assistant, ...state.assistants]
  })),

  updateAssistant: (id, assistant) => set((state) => ({
    assistants: state.assistants.map((a) => (a.id === id ? assistant : a)),
    currentAssistant: state.currentAssistant?.id === id ? assistant : state.currentAssistant
  })),

  removeAssistant: (id) => set((state) => ({
    assistants: state.assistants.filter((a) => a.id !== id),
    currentAssistant: state.currentAssistant?.id === id ? null : state.currentAssistant
  })),
}))