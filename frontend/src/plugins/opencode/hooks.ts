import { useEffect, useRef, useCallback } from 'react'
import { pluginAdapter } from './adapter'
import { toolRegistry } from './toolRegistry'
import { PluginEvent, PluginContext, OpenCodePluginConfig } from './types'

/**
 * Hook to use the OpenCode plugin adapter
 */
export function useOpenCodePlugin() {
  const initialized = useRef(false)

  useEffect(() => {
    if (!initialized.current) {
      pluginAdapter.initialize()
      initialized.current = true
    }
  }, [])

  const registerPlugin = useCallback((config: OpenCodePluginConfig) => {
    pluginAdapter.registerPlugin(config)
  }, [])

  const unregisterPlugin = useCallback((pluginId: string) => {
    pluginAdapter.unregisterPlugin(pluginId)
  }, [])

  const getPlugins = useCallback(() => {
    return pluginAdapter.getPlugins()
  }, [])

  const emitEvent = useCallback((event: string, data: unknown, context?: PluginContext) => {
    pluginAdapter.emitEvent(event, data, context)
  }, [])

  return {
    registerPlugin,
    unregisterPlugin,
    getPlugins,
    emitEvent,
    pluginAdapter
  }
}

/**
 * Hook to subscribe to plugin events
 */
export function usePluginEvent(
  event: string,
  handler: (event: PluginEvent) => void
) {
  useEffect(() => {
    const unsubscribe = pluginAdapter.onEvent(event, handler)
    return unsubscribe
  }, [event, handler])
}

/**
 * Hook to use the tool registry
 */
export function useToolRegistry() {
  const registerTool = useCallback((name: string, description: string, parameters: unknown, handler: (args: Record<string, unknown>) => Promise<unknown>) => {
    toolRegistry.registerTool({
      name,
      description,
      parameters: parameters as any,
      handler
    })
  }, [])

  const unregisterTool = useCallback((toolName: string) => {
    toolRegistry.unregisterTool(toolName)
  }, [])

  const executeTool = useCallback(async (toolName: string, args: Record<string, unknown>, context?: PluginContext) => {
    return toolRegistry.executeTool(toolName, args, context)
  }, [])

  const getToolDefinitions = useCallback(() => {
    return toolRegistry.getToolDefinitions()
  }, [])

  return {
    registerTool,
    unregisterTool,
    executeTool,
    getToolDefinitions,
    toolRegistry
  }
}

/**
 * Hook to integrate plugins with chat functionality
 */
export function usePluginChatIntegration() {
  const { emitEvent } = useOpenCodePlugin()
  const { executeTool, getToolDefinitions } = useToolRegistry()

  const onMessageSent = useCallback((message: {
    conversationId: number
    content: string
    role: 'user' | 'assistant'
  }) => {
    emitEvent('message_sent', message, {
      conversationId: message.conversationId
    })
  }, [emitEvent])

  const onMessageReceived = useCallback((message: {
    conversationId: number
    content: string
    role: 'user' | 'assistant'
  }) => {
    emitEvent('message_received', message, {
      conversationId: message.conversationId
    })
  }, [emitEvent])

  const onStreamChunk = useCallback((conversationId: number, chunk: string) => {
    emitEvent('stream_chunk', { conversationId, chunk }, { conversationId })
  }, [emitEvent])

  const onThinking = useCallback((conversationId: number, thinking: string) => {
    emitEvent('thinking', { conversationId, thinking }, { conversationId })
  }, [emitEvent])

  const onToolCall = useCallback((conversationId: number, toolCall: {
    id: string
    name: string
    arguments: Record<string, unknown>
  }) => {
    emitEvent('tool_call', { conversationId, toolCall }, { conversationId })
  }, [emitEvent])

  const handleToolCall = useCallback(async (toolCall: {
    id: string
    name: string
    arguments: string
  }, context?: PluginContext) => {
    try {
      const args = JSON.parse(toolCall.arguments)
      const result = await executeTool(toolCall.name, args, context)
      return {
        toolCallId: toolCall.id,
        result: JSON.stringify(result)
      }
    } catch (error) {
      console.error('Tool call failed:', error)
      return {
        toolCallId: toolCall.id,
        error: error instanceof Error ? error.message : 'Tool execution failed'
      }
    }
  }, [executeTool])

  return {
    onMessageSent,
    onMessageReceived,
    onStreamChunk,
    onThinking,
    onToolCall,
    handleToolCall,
    getToolDefinitions
  }
}