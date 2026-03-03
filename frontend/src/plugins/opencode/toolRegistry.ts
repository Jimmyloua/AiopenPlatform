import { OpenCodeTool, PluginContext, JSONSchema } from './types'
import { pluginAdapter } from './adapter'

/**
 * Tool Registry
 * Manages tools that can be executed by OpenCode plugins
 */
export class ToolRegistry {
  private tools: Map<string, OpenCodeTool> = new Map()

  /**
   * Register a tool
   */
  registerTool(tool: OpenCodeTool): void {
    this.tools.set(tool.name, tool)
    console.log(`[ToolRegistry] Registered tool: ${tool.name}`)
  }

  /**
   * Unregister a tool
   */
  unregisterTool(toolName: string): void {
    this.tools.delete(toolName)
    console.log(`[ToolRegistry] Unregistered tool: ${toolName}`)
  }

  /**
   * Get a tool by name
   */
  getTool(toolName: string): OpenCodeTool | undefined {
    return this.tools.get(toolName)
  }

  /**
   * Get all registered tools
   */
  getAllTools(): OpenCodeTool[] {
    return Array.from(this.tools.values())
  }

  /**
   * Get tool definitions in OpenAI format
   */
  getToolDefinitions(): Array<{
    type: 'function'
    function: {
      name: string
      description: string
      parameters: JSONSchema
    }
  }> {
    return this.getAllTools().map(tool => ({
      type: 'function' as const,
      function: {
        name: tool.name,
        description: tool.description,
        parameters: tool.parameters
      }
    }))
  }

  /**
   * Execute a tool
   */
  async executeTool(
    toolName: string,
    args: Record<string, unknown>,
    context?: PluginContext
  ): Promise<unknown> {
    const tool = this.tools.get(toolName)
    if (!tool) {
      throw new Error(`Tool not found: ${toolName}`)
    }

    if (!tool.handler) {
      throw new Error(`Tool has no handler: ${toolName}`)
    }

    console.log(`[ToolRegistry] Executing tool: ${toolName}`, args)

    try {
      const result = await tool.handler(args)

      // Emit tool execution event
      pluginAdapter.emitEvent('tool_executed', {
        toolName,
        args,
        result,
        context
      })

      return result
    } catch (error) {
      console.error(`[ToolRegistry] Tool execution failed: ${toolName}`, error)
      throw error
    }
  }

  /**
   * Check if a tool exists
   */
  hasTool(toolName: string): boolean {
    return this.tools.has(toolName)
  }
}

// Singleton instance
export const toolRegistry = new ToolRegistry()

// Register default tools
registerDefaultTools()

function registerDefaultTools() {
  // File read tool
  toolRegistry.registerTool({
    name: 'read_file',
    description: 'Read the contents of a file',
    parameters: {
      type: 'object',
      properties: {
        path: {
          type: 'string',
          description: 'The path to the file to read'
        }
      },
      required: ['path']
    },
    handler: async (args) => {
      // This would call the backend API
      console.log('Reading file:', args.path)
      return { content: `File content for ${args.path}` }
    }
  })

  // Web search tool
  toolRegistry.registerTool({
    name: 'web_search',
    description: 'Search the web for information',
    parameters: {
      type: 'object',
      properties: {
        query: {
          type: 'string',
          description: 'The search query'
        },
        numResults: {
          type: 'number',
          description: 'Number of results to return',
          default: 5
        }
      },
      required: ['query']
    },
    handler: async (args) => {
      console.log('Web search:', args.query)
      return { results: [] }
    }
  })

  // Code execution tool
  toolRegistry.registerTool({
    name: 'execute_code',
    description: 'Execute code in a sandboxed environment',
    parameters: {
      type: 'object',
      properties: {
        language: {
          type: 'string',
          enum: ['python', 'javascript', 'bash'],
          description: 'The programming language'
        },
        code: {
          type: 'string',
          description: 'The code to execute'
        }
      },
      required: ['language', 'code']
    },
    handler: async (args) => {
      console.log('Executing code:', args.language)
      return { output: 'Code execution result' }
    }
  })
}