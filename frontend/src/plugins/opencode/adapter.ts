import { OpenCodePluginConfig, PluginMessage, PluginCommand, PluginEvent, PluginContext } from './types'

/**
 * OpenCode Plugin Adapter
 * Provides integration between the CUI frontend and OpenCode plugins
 */
export class OpenCodePluginAdapter {
  private plugins: Map<string, OpenCodePluginConfig> = new Map()
  private messageHandlers: Map<string, (message: PluginMessage) => void> = new Map()
  private eventListeners: Map<string, Set<(event: PluginEvent) => void>> = new Map()
  private initialized = false

  constructor() {
    this.setupMessageListener()
  }

  /**
   * Initialize the plugin adapter
   */
  async initialize(): Promise<void> {
    if (this.initialized) return

    // Load registered plugins
    await this.loadPlugins()

    // Notify all plugins that we're ready
    this.broadcast({ type: 'plugin:ready', payload: { timestamp: Date.now() } })

    this.initialized = true
    console.log('[OpenCodePluginAdapter] Initialized')
  }

  /**
   * Register a new plugin
   */
  registerPlugin(config: OpenCodePluginConfig): void {
    this.plugins.set(config.pluginId, config)
    console.log(`[OpenCodePluginAdapter] Registered plugin: ${config.pluginName} (${config.pluginId})`)

    // Send init message to plugin
    this.sendToPlugin(config.pluginId, {
      type: 'plugin:init',
      payload: { config }
    })
  }

  /**
   * Unregister a plugin
   */
  unregisterPlugin(pluginId: string): void {
    this.plugins.delete(pluginId)
    this.messageHandlers.delete(pluginId)
    console.log(`[OpenCodePluginAdapter] Unregistered plugin: ${pluginId}`)
  }

  /**
   * Get all registered plugins
   */
  getPlugins(): OpenCodePluginConfig[] {
    return Array.from(this.plugins.values())
  }

  /**
   * Get a specific plugin
   */
  getPlugin(pluginId: string): OpenCodePluginConfig | undefined {
    return this.plugins.get(pluginId)
  }

  /**
   * Check if a plugin is enabled
   */
  isPluginEnabled(pluginId: string): boolean {
    const plugin = this.plugins.get(pluginId)
    return plugin?.enabled ?? false
  }

  /**
   * Enable or disable a plugin
   */
  setPluginEnabled(pluginId: string, enabled: boolean): void {
    const plugin = this.plugins.get(pluginId)
    if (plugin) {
      plugin.enabled = enabled
      console.log(`[OpenCodePluginAdapter] Plugin ${pluginId} ${enabled ? 'enabled' : 'disabled'}`)
    }
  }

  /**
   * Send a command to a plugin
   */
  async sendCommand(pluginId: string, command: PluginCommand): Promise<unknown> {
    const correlationId = command.correlationId || this.generateCorrelationId()

    const message: PluginMessage = {
      type: 'plugin:command',
      pluginId,
      payload: command,
      timestamp: Date.now(),
      correlationId
    }

    return new Promise((resolve, reject) => {
      // Set up response handler
      const timeout = setTimeout(() => {
        this.messageHandlers.delete(correlationId)
        reject(new Error(`Plugin command timeout: ${command.command}`))
      }, 30000)

      this.messageHandlers.set(correlationId, (response) => {
        clearTimeout(timeout)
        if (response.type === 'plugin:error') {
          reject(new Error(response.payload as string))
        } else {
          resolve(response.payload)
        }
      })

      this.sendToPlugin(pluginId, message)
    })
  }

  /**
   * Emit an event to all plugins
   */
  emitEvent(event: string, data: unknown, context?: PluginContext): void {
    const pluginEvent: PluginEvent = {
      event,
      data,
      timestamp: Date.now()
    }

    // Broadcast to all enabled plugins
    this.plugins.forEach((plugin, pluginId) => {
      if (plugin.enabled) {
        this.sendToPlugin(pluginId, {
          type: 'plugin:event:chat',
          payload: { event: pluginEvent, context }
        })
      }
    })

    // Notify local event listeners
    const listeners = this.eventListeners.get(event)
    if (listeners) {
      listeners.forEach(listener => listener(pluginEvent))
    }
  }

  /**
   * Subscribe to plugin events
   */
  onEvent(event: string, listener: (event: PluginEvent) => void): () => void {
    if (!this.eventListeners.has(event)) {
      this.eventListeners.set(event, new Set())
    }
    this.eventListeners.get(event)!.add(listener)

    // Return unsubscribe function
    return () => {
      this.eventListeners.get(event)?.delete(listener)
    }
  }

  /**
   * Handle messages from plugins
   */
  handleMessage(message: PluginMessage): void {
    console.log(`[OpenCodePluginAdapter] Received message: ${message.type}`)

    // Handle response messages
    if (message.correlationId && this.messageHandlers.has(message.correlationId)) {
      const handler = this.messageHandlers.get(message.correlationId)
      if (handler) {
        handler(message)
        this.messageHandlers.delete(message.correlationId)
      }
      return
    }

    // Handle other message types
    switch (message.type) {
      case 'plugin:message':
        this.handlePluginMessage(message)
        break
      case 'plugin:error':
        console.error('[OpenCodePluginAdapter] Plugin error:', message.payload)
        break
      default:
        console.warn('[OpenCodePluginAdapter] Unknown message type:', message.type)
    }
  }

  /**
   * Handle a message from a plugin
   */
  private handlePluginMessage(message: PluginMessage): void {
    const plugin = this.plugins.get(message.pluginId || '')
    if (!plugin || !plugin.enabled) {
      return
    }

    // Emit to event listeners
    const payload = message.payload as { event?: string; data?: unknown }
    if (payload.event) {
      this.emitEvent(payload.event, payload.data)
    }
  }

  /**
   * Send message to a specific plugin
   */
  private sendToPlugin(pluginId: string, message: PluginMessage): void {
    message.pluginId = pluginId

    // In a real implementation, this would use postMessage or a similar mechanism
    // For now, we'll use a custom event system
    window.dispatchEvent(new CustomEvent('opencode:plugin:message', {
      detail: message
    }))

    console.log(`[OpenCodePluginAdapter] Sent message to plugin ${pluginId}:`, message.type)
  }

  /**
   * Broadcast message to all plugins
   */
  private broadcast(message: PluginMessage): void {
    this.plugins.forEach((plugin, pluginId) => {
      if (plugin.enabled) {
        this.sendToPlugin(pluginId, message)
      }
    })
  }

  /**
   * Set up window message listener for plugin communication
   */
  private setupMessageListener(): void {
    window.addEventListener('message', (event) => {
      // Validate origin for security
      // if (event.origin !== expectedOrigin) return

      const message = event.data as PluginMessage
      if (message && message.type?.startsWith('plugin:')) {
        this.handleMessage(message)
      }
    })

    // Also listen for custom events
    window.addEventListener('opencode:plugin:response', ((event: CustomEvent) => {
      this.handleMessage(event.detail)
    }) as EventListener)
  }

  /**
   * Load plugins from storage or configuration
   */
  private async loadPlugins(): Promise<void> {
    // Load from localStorage for now
    const stored = localStorage.getItem('opencode:plugins')
    if (stored) {
      try {
        const plugins = JSON.parse(stored) as OpenCodePluginConfig[]
        plugins.forEach(plugin => {
          this.plugins.set(plugin.pluginId, plugin)
        })
        console.log(`[OpenCodePluginAdapter] Loaded ${plugins.length} plugins from storage`)
      } catch (e) {
        console.error('[OpenCodePluginAdapter] Failed to load plugins:', e)
      }
    }
  }

  /**
   * Save plugins to storage
   */
  savePlugins(): void {
    const plugins = Array.from(this.plugins.values())
    localStorage.setItem('opencode:plugins', JSON.stringify(plugins))
  }

  /**
   * Generate a unique correlation ID
   */
  private generateCorrelationId(): string {
    return `corr_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }
}

// Singleton instance
export const pluginAdapter = new OpenCodePluginAdapter()