# OpenCode Plugin Integration

This module provides integration between the AI Open Platform frontend and OpenCode plugins.

## Features

- **Plugin Management**: Register, enable/disable, and manage OpenCode plugins
- **Tool Registry**: Register and execute tools that can be used by AI assistants
- **Event System**: Emit and subscribe to plugin events
- **Chat Integration**: Hooks for integrating plugins with chat functionality

## Usage

### Basic Plugin Setup

```typescript
import { useOpenCodePlugin } from '@/plugins/opencode'

function MyComponent() {
  const { registerPlugin, getPlugins, emitEvent } = useOpenCodePlugin()

  useEffect(() => {
    registerPlugin({
      pluginId: 'my-plugin',
      pluginName: 'My Plugin',
      version: '1.0.0',
      enabled: true,
      permissions: ['read:messages', 'write:messages']
    })
  }, [])

  return <div>Plugin registered</div>
}
```

### Registering Custom Tools

```typescript
import { useToolRegistry } from '@/plugins/opencode'

function MyComponent() {
  const { registerTool, executeTool } = useToolRegistry()

  useEffect(() => {
    registerTool(
      'my_custom_tool',
      'A custom tool description',
      {
        type: 'object',
        properties: {
          input: { type: 'string', description: 'Input parameter' }
        },
        required: ['input']
      },
      async (args) => {
        // Tool implementation
        return { result: `Processed: ${args.input}` }
      }
    )
  }, [])

  return <div>Tool registered</div>
}
```

### Chat Integration

```typescript
import { usePluginChatIntegration } from '@/plugins/opencode'

function ChatComponent() {
  const {
    onMessageSent,
    onMessageReceived,
    onStreamChunk,
    handleToolCall,
    getToolDefinitions
  } = usePluginChatIntegration()

  // Use these callbacks in your chat implementation
}
```

### Event Subscription

```typescript
import { usePluginEvent } from '@/plugins/opencode'

function MyComponent() {
  usePluginEvent('message_sent', (event) => {
    console.log('Message sent:', event.data)
  })

  return <div>Listening to events</div>
}
```

## Message Types

- `plugin:init` - Plugin initialization
- `plugin:ready` - Plugin ready notification
- `plugin:message` - General message
- `plugin:command` - Command execution request
- `plugin:response` - Command response
- `plugin:error` - Error notification

## Event Types

- `message_sent` - User sent a message
- `message_received` - Assistant response received
- `stream_chunk` - Streaming chunk received
- `thinking` - Thinking process data
- `tool_call` - Tool execution request
- `tool_executed` - Tool execution completed