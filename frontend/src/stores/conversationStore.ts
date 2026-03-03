import { create } from 'zustand'
import { Conversation, Message, Assistant } from '../types'

interface ConversationState {
  conversations: Conversation[]
  currentConversation: Conversation | null
  messages: Message[]
  selectedAssistant: Assistant | null
  isLoading: boolean
  setConversations: (conversations: Conversation[]) => void
  setCurrentConversation: (conversation: Conversation | null) => void
  setMessages: (messages: Message[]) => void
  addMessage: (message: Message) => void
  setSelectedAssistant: (assistant: Assistant | null) => void
  clearMessages: () => void
}

export const useConversationStore = create<ConversationState>((set) => ({
  conversations: [],
  currentConversation: null,
  messages: [],
  selectedAssistant: null,
  isLoading: false,

  setConversations: (conversations) => set({ conversations }),

  setCurrentConversation: (conversation) => set({ currentConversation: conversation }),

  setMessages: (messages) => set({ messages }),

  addMessage: (message) => set((state) => ({ messages: [...state.messages, message] })),

  setSelectedAssistant: (assistant) => set({ selectedAssistant: assistant }),

  clearMessages: () => set({ messages: [] }),
}))