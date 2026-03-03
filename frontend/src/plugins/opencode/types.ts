// OpenCode Plugin Types
// Defines the interface for OpenCode plugin integration

/**
 * OpenCode Plugin Configuration
 */
export interface OpenCodePluginConfig {
  pluginId: string
  pluginName: string
  version: string
  enabled: boolean
  permissions: PluginPermission[]
  settings?: Record<string, unknown>
}

/**
 * Plugin Permission types
 */
export type PluginPermission =
  | 'read:messages'
  | 'write:messages'
  | 'read:conversations'
  | 'write:conversations'
  | 'read:assistants'
  | 'write:assistants'
  | 'execute:tools'
  | 'access:filesystem'
  | 'access:network'

/**
 * Plugin Message - Message format for plugin communication
 */
export interface PluginMessage {
  type: PluginMessageType
  pluginId?: string
  payload: unknown
  timestamp: number
  correlationId?: string
}

/**
 * Plugin Message Types
 */
export type PluginMessageType =
  // Lifecycle
  | 'plugin:init'
  | 'plugin:ready'
  | 'plugin:shutdown'
  // Communication
  | 'plugin:message'
  | 'plugin:command'
  | 'plugin:response'
  | 'plugin:error'
  // Events
  | 'plugin:event:chat'
  | 'plugin:event:thinking'
  | 'plugin:event:tool_call'
  | 'plugin:event:stream'

/**
 * Plugin Command - Commands that can be sent to plugins
 */
export interface PluginCommand {
  command: string
  args?: Record<string, unknown>
  correlationId?: string
}

/**
 * Plugin Event - Events from OpenCode to plugins
 */
export interface PluginEvent {
  event: string
  data: unknown
  timestamp: number
}

/**
 * OpenCode Tool Definition
 */
export interface OpenCodeTool {
  name: string
  description: string
  parameters: JSONSchema
  handler?: (args: Record<string, unknown>) => Promise<unknown>
}

/**
 * Simple JSON Schema type
 */
export interface JSONSchema {
  type: string
  properties?: Record<string, JSONSchema>
  required?: string[]
  items?: JSONSchema
  enum?: string[]
  description?: string
  default?: unknown
}

/**
 * Plugin Context - Context passed to plugin handlers
 */
export interface PluginContext {
  conversationId?: number
  assistantId?: number
  userId?: number
  message?: Message
  [key: string]: unknown
}

/**
 * Message type for plugin context
 */
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