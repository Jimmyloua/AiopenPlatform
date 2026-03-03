import SockJS from 'sockjs-client'
import { Client, IMessage, StompHeaders } from '@stomp/stompjs'
import { useUserStore } from '../stores/userStore'

export interface WebSocketMessage {
  type: string
  payload: unknown
  timestamp: number
  correlationId?: string
}

export type MessageHandler = (message: WebSocketMessage) => void

/**
 * WebSocket Service
 * Manages WebSocket connection for real-time communication
 */
export class WebSocketService {
  private client: Client | null = null
  private connected = false
  private subscriptions: Map<string, { id: string; handler: MessageHandler }> = new Map()
  private messageQueue: WebSocketMessage[] = []
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000

  /**
   * Connect to WebSocket server
   */
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.connected && this.client) {
        resolve()
        return
      }

      const token = useUserStore.getState().token

      const socketUrl = `${window.location.origin}/ws`
      console.log('[WebSocket] Connecting to:', socketUrl)

      this.client = new Client({
        webSocketFactory: () => new SockJS(socketUrl) as WebSocket,
        connectHeaders: {
          Authorization: `Bearer ${token}`
        } as StompHeaders,
        debug: (str) => {
          console.log('[STOMP]', str)
        },
        reconnectDelay: this.reconnectDelay,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,

        onConnect: () => {
          console.log('[WebSocket] Connected')
          this.connected = true
          this.reconnectAttempts = 0

          // Resubscribe to all topics
          this.subscriptions.forEach((sub, destination) => {
            this.subscribeInternal(destination, sub.handler)
          })

          // Send queued messages
          this.flushMessageQueue()

          resolve()
        },

        onDisconnect: () => {
          console.log('[WebSocket] Disconnected')
          this.connected = false
        },

        onStompError: (frame) => {
          console.error('[WebSocket] STOMP error:', frame)
          reject(new Error(frame.headers.message))
        },

        onWebSocketError: (event) => {
          console.error('[WebSocket] WebSocket error:', event)
          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++
            console.log(`[WebSocket] Reconnecting... attempt ${this.reconnectAttempts}`)
          } else {
            reject(new Error('WebSocket connection failed'))
          }
        }
      })

      this.client.activate()
    })
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect(): void {
    if (this.client) {
      this.client.deactivate()
      this.client = null
      this.connected = false
      this.subscriptions.clear()
      console.log('[WebSocket] Disconnected')
    }
  }

  /**
   * Subscribe to a topic
   */
  subscribe(destination: string, handler: MessageHandler): () => void {
    if (this.subscriptions.has(destination)) {
      console.warn(`[WebSocket] Already subscribed to ${destination}`)
      return () => this.unsubscribe(destination)
    }

    this.subscriptions.set(destination, { id: '', handler })

    if (this.connected && this.client) {
      this.subscribeInternal(destination, handler)
    }

    return () => this.unsubscribe(destination)
  }

  /**
   * Internal subscribe implementation
   */
  private subscribeInternal(destination: string, handler: MessageHandler): void {
    if (!this.client) return

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const body = JSON.parse(message.body) as WebSocketMessage
        handler(body)
      } catch (e) {
        console.error('[WebSocket] Failed to parse message:', e)
      }
    })

    const existing = this.subscriptions.get(destination)
    if (existing) {
      this.subscriptions.set(destination, { ...existing, id: subscription.id })
    }

    console.log(`[WebSocket] Subscribed to ${destination}`)
  }

  /**
   * Unsubscribe from a topic
   */
  unsubscribe(destination: string): void {
    const sub = this.subscriptions.get(destination)
    if (sub && this.client) {
      this.client.unsubscribe(sub.id)
      this.subscriptions.delete(destination)
      console.log(`[WebSocket] Unsubscribed from ${destination}`)
    }
  }

  /**
   * Send a message to a destination
   */
  send(destination: string, message: WebSocketMessage): void {
    if (!this.connected || !this.client) {
      console.log('[WebSocket] Not connected, queuing message')
      this.messageQueue.push(message)
      return
    }

    this.client.publish({
      destination,
      body: JSON.stringify(message)
    })
  }

  /**
   * Send queued messages
   */
  private flushMessageQueue(): void {
    while (this.messageQueue.length > 0 && this.connected) {
      const message = this.messageQueue.shift()
      if (message) {
        this.send('/app/chat.request', message)
      }
    }
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.connected
  }

  /**
   * Send chat request
   */
  sendChatRequest(conversationId: number, content: string): void {
    const message: WebSocketMessage = {
      type: 'CHAT_REQUEST',
      payload: {
        conversationId,
        content,
        role: 'user'
      },
      timestamp: Date.now()
    }

    this.send('/app/chat.request', message)
  }

  /**
   * Subscribe to conversation updates
   */
  subscribeToConversation(conversationId: number, handler: MessageHandler): () => void {
    return this.subscribe(`/topic/conversation.${conversationId}`, handler)
  }

  /**
   * Subscribe to streaming responses
   */
  subscribeToStream(conversationId: number, handler: MessageHandler): () => void {
    return this.subscribe(`/topic/chat.stream.${conversationId}`, handler)
  }

  /**
   * Subscribe to user-specific messages
   */
  subscribeToUser(handler: MessageHandler): () => void {
    return this.subscribe('/user/queue/messages', handler)
  }

  /**
   * Subscribe to errors
   */
  subscribeToErrors(handler: MessageHandler): () => void {
    return this.subscribe('/user/queue/errors', handler)
  }
}

// Singleton instance
export const websocketService = new WebSocketService()