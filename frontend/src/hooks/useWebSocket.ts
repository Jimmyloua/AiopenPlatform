import { useEffect, useRef, useCallback } from 'react'
import { websocketService, WebSocketMessage } from '../services/websocket'

/**
 * Hook to use WebSocket connection
 */
export function useWebSocket() {
  const connected = useRef(false)

  useEffect(() => {
    if (!connected.current) {
      websocketService.connect().catch(console.error)
      connected.current = true
    }

    return () => {
      // Don't disconnect on unmount, keep connection for the app lifetime
    }
  }, [])

  const subscribe = useCallback((destination: string, handler: (message: WebSocketMessage) => void) => {
    return websocketService.subscribe(destination, handler)
  }, [])

  const send = useCallback((destination: string, message: WebSocketMessage) => {
    websocketService.send(destination, message)
  }, [])

  const sendChatRequest = useCallback((conversationId: number, content: string) => {
    websocketService.sendChatRequest(conversationId, content)
  }, [])

  return {
    isConnected: websocketService.isConnected(),
    subscribe,
    send,
    sendChatRequest,
    websocketService
  }
}

/**
 * Hook to subscribe to a WebSocket topic
 */
export function useWebSocketSubscription(
  destination: string | null,
  handler: (message: WebSocketMessage) => void
) {
  useEffect(() => {
    if (!destination) return

    const unsubscribe = websocketService.subscribe(destination, handler)
    return unsubscribe
  }, [destination, handler])
}

/**
 * Hook to subscribe to conversation updates
 */
export function useConversationSubscription(
  conversationId: number | null,
  onMessage: (message: WebSocketMessage) => void
) {
  useWebSocketSubscription(
    conversationId ? `/topic/conversation.${conversationId}` : null,
    onMessage
  )
}

/**
 * Hook to subscribe to streaming responses
 */
export function useStreamSubscription(
  conversationId: number | null,
  onChunk: (chunk: string) => void,
  onComplete?: () => void
) {
  const handler = useCallback((message: WebSocketMessage) => {
    if (message.type === 'CHAT_STREAM') {
      const payload = message.payload as { chunk?: string }
      if (payload.chunk) {
        onChunk(payload.chunk)
      }
    } else if (message.type === 'CHAT_RESPONSE' && onComplete) {
      onComplete()
    }
  }, [onChunk, onComplete])

  useWebSocketSubscription(
    conversationId ? `/topic/chat.stream.${conversationId}` : null,
    handler
  )
}