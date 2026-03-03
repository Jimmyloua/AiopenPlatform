import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { User, AuthResponse } from '../types'
import { authApi } from '../services/api'

interface UserState {
  token: string | null
  user: User | null
  isLoading: boolean
  error: string | null
  login: (username: string, password: string) => Promise<void>
  register: (username: string, email: string, password: string, nickname?: string) => Promise<void>
  logout: () => void
  fetchUser: () => Promise<void>
  clearError: () => void
}

export const useUserStore = create<UserState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      isLoading: false,
      error: null,

      login: async (username: string, password: string) => {
        set({ isLoading: true, error: null })
        try {
          const response = await authApi.login(username, password)
          if (response.success && response.data) {
            set({ token: response.data.token, user: response.data.user, isLoading: false })
          } else {
            set({ error: response.message || '登录失败', isLoading: false })
          }
        } catch (error) {
          set({ error: (error as Error).message, isLoading: false })
        }
      },

      register: async (username: string, email: string, password: string, nickname?: string) => {
        set({ isLoading: true, error: null })
        try {
          const response = await authApi.register(username, email, password, nickname)
          if (response.success && response.data) {
            set({ token: response.data.token, user: response.data.user, isLoading: false })
          } else {
            set({ error: response.message || '注册失败', isLoading: false })
          }
        } catch (error) {
          set({ error: (error as Error).message, isLoading: false })
        }
      },

      logout: () => {
        set({ token: null, user: null })
      },

      fetchUser: async () => {
        const { token } = get()
        if (!token) return

        set({ isLoading: true })
        try {
          const response = await authApi.getCurrentUser()
          if (response.success && response.data) {
            set({ user: response.data, isLoading: false })
          }
        } catch (error) {
          set({ error: (error as Error).message, isLoading: false })
        }
      },

      clearError: () => set({ error: null }),
    }),
    {
      name: 'user-storage',
      partialize: (state) => ({ token: state.token, user: state.user }),
    }
  )
)