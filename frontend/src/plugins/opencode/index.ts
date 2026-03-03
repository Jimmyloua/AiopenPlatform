// OpenCode Plugin Integration
// This module provides integration with OpenCode plugins

// Types
export * from './types'

// Core components
export { OpenCodePluginAdapter, pluginAdapter } from './adapter'
export { ToolRegistry, toolRegistry } from './toolRegistry'

// React hooks
export {
  useOpenCodePlugin,
  usePluginEvent,
  useToolRegistry,
  usePluginChatIntegration
} from './hooks'