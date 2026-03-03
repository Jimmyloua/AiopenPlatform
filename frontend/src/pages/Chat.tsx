import { useState, useRef, useEffect, useCallback } from 'react'
import { Input, Button, Spin, Empty, Select, Space, Card, message as antdMessage } from 'antd'
import { SendOutlined, ClearOutlined, ApiOutlined, ThunderboltOutlined } from '@ant-design/icons'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { useConversationStore } from '../../stores/conversationStore'
import { useAssistantStore } from '../../stores/assistantStore'
import { conversationApi, openaiApi } from '../../services/api'
import { useStreamSubscription, useWebSocket } from '../../hooks/useWebSocket'
import { usePluginChatIntegration } from '../../plugins/opencode'
import { Message, ChatCompletionMessage } from '../../types'
import './Chat.css'

const { TextArea } = Input

const Chat = () => {
  const {
    currentConversation,
    messages,
    selectedAssistant,
    setCurrentConversation,
    setMessages,
    addMessage,
    setSelectedAssistant,
  } = useConversationStore()
  const { assistants, setAssistants } = useAssistantStore()
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [streamingMessage, setStreamingMessage] = useState('')
  const [useWebSocket, setUseWebSocket] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // Plugin integration
  const {
    onMessageSent,
    onMessageReceived,
    onStreamChunk,
    onThinking,
    onToolCall,
    handleToolCall,
    getToolDefinitions
  } = usePluginChatIntegration()

  // WebSocket streaming
  const { sendChatRequest, isConnected: wsConnected } = useWebSocket()

  useEffect(() => {
    loadAssistants()
  }, [])

  useEffect(() => {
    scrollToBottom()
  }, [messages, streamingMessage])

  // WebSocket stream subscription
  useStreamSubscription(
    currentConversation?.id || null,
    (chunk) => {
      setStreamingMessage(prev => prev + chunk)
      onStreamChunk(currentConversation?.id || 0, chunk)
    },
    () => {
      if (streamingMessage && currentConversation) {
        // Save the complete message
        conversationApi.addMessage(currentConversation.id, {
          conversationId: currentConversation.id,
          content: streamingMessage,
        }).then(response => {
          if (response.success && response.data) {
            addMessage(response.data)
            onMessageReceived({
              conversationId: currentConversation.id,
              content: streamingMessage,
              role: 'assistant'
            })
          }
        }).catch(console.error)
      }
      setStreamingMessage('')
      setLoading(false)
    }
  )

  const loadAssistants = async () => {
    try {
      const response = await conversationApi.list()
      if (response.success && response.data) {
        setAssistants(response.data.records)
      }
    } catch (error) {
      console.error('Failed to load assistants:', error)
    }
  }

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  const handleAssistantChange = async (assistantId: number) => {
    const assistant = assistants.find((a) => a.id === assistantId)
    if (assistant) {
      setSelectedAssistant(assistant)
      try {
        const response = await conversationApi.create({ assistantId })
        if (response.success && response.data) {
          setCurrentConversation(response.data)
          setMessages([])
        }
      } catch (error) {
        console.error('Failed to create conversation:', error)
      }
    }
  }

  const handleSend = async () => {
    if (!input.trim() || loading) return

    const userMessage = input.trim()
    setInput('')
    setLoading(true)

    try {
      // Add user message
      if (currentConversation) {
        const response = await conversationApi.addMessage(currentConversation.id, {
          conversationId: currentConversation.id,
          content: userMessage,
        })
        if (response.success && response.data) {
          addMessage(response.data)
          onMessageSent({
            conversationId: currentConversation.id,
            content: userMessage,
            role: 'user'
          })
        }
      }

      // Prepare messages for API
      const chatMessages: ChatCompletionMessage[] = [
        ...(selectedAssistant?.systemPrompt
          ? [{ role: 'system' as const, content: selectedAssistant.systemPrompt }]
          : []),
        ...messages.map((m) => ({
          role: m.role as 'user' | 'assistant' | 'system',
          content: m.content || '',
        })),
        { role: 'user', content: userMessage },
      ]

      // Get tool definitions from plugins
      const tools = getToolDefinitions()

      // Stream response
      setStreamingMessage('')

      if (useWebSocket && wsConnected) {
        // Use WebSocket for streaming
        sendChatRequest(currentConversation?.id || 0, userMessage)
      } else {
        // Use HTTP streaming
        const stream = openaiApi.streamChatCompletions({
          model: selectedAssistant?.modelConfig
            ? JSON.parse(selectedAssistant.modelConfig).model || 'gpt-4'
            : 'gpt-4',
          messages: chatMessages,
          stream: true,
          tools: tools.length > 0 ? tools : undefined,
        })

        let fullContent = ''
        let thinkingContent = ''
        let currentToolCall: { id: string; name: string; arguments: string } | null = null

        for await (const chunk of stream) {
          const delta = chunk.choices[0]?.delta

          // Handle content
          if (delta?.content) {
            fullContent += delta.content
            setStreamingMessage(fullContent)
            onStreamChunk(currentConversation?.id || 0, delta.content)
          }

          // Handle thinking (Claude-style)
          if ((delta as any)?.thinking) {
            thinkingContent += (delta as any).thinking
            onThinking(currentConversation?.id || 0, thinkingContent)
          }

          // Handle tool calls
          if (delta?.toolCalls && delta.toolCalls.length > 0) {
            for (const tc of delta.toolCalls) {
              if (tc.id) {
                // New tool call
                if (currentToolCall && currentToolCall.arguments) {
                  // Execute previous tool call
                  await executeToolCall(currentToolCall)
                }
                currentToolCall = {
                  id: tc.id,
                  name: tc.function?.name || '',
                  arguments: tc.function?.arguments || ''
                }
              } else if (currentToolCall && tc.function?.arguments) {
                // Continue tool call arguments
                currentToolCall.arguments += tc.function.arguments
              }
            }
          }
        }

        // Execute final tool call if any
        if (currentToolCall && currentToolCall.arguments) {
          await executeToolCall(currentToolCall)
        }

        // Add assistant message
        if (currentConversation && fullContent) {
          const response = await conversationApi.addMessage(currentConversation.id, {
            conversationId: currentConversation.id,
            content: fullContent,
          })
          if (response.success && response.data) {
            addMessage(response.data)
            onMessageReceived({
              conversationId: currentConversation.id,
              content: fullContent,
              role: 'assistant'
            })
          }
        }

        setStreamingMessage('')
      }
    } catch (error) {
      console.error('Failed to send message:', error)
      antdMessage.error('发送消息失败')
    } finally {
      setLoading(false)
    }
  }

  const executeToolCall = async (toolCall: { id: string; name: string; arguments: string }) => {
    onToolCall(currentConversation?.id || 0, toolCall)

    try {
      const result = await handleToolCall(toolCall, {
        conversationId: currentConversation?.id
      })

      // Add tool result to messages (would need backend support)
      console.log('Tool result:', result)
    } catch (error) {
      console.error('Tool execution failed:', error)
    }
  }

  const handleClear = () => {
    setCurrentConversation(null)
    setMessages([])
    setSelectedAssistant(null)
    setStreamingMessage('')
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="chat-container">
      <div className="chat-header">
        <Space>
          <Select
            placeholder="选择助手"
            value={selectedAssistant?.id}
            onChange={handleAssistantChange}
            style={{ width: 200 }}
            options={assistants.map((a) => ({ value: a.id, label: a.name }))}
          />
          <Button icon={<ClearOutlined />} onClick={handleClear}>
            清空对话
          </Button>
          <Button
            type={useWebSocket ? 'primary' : 'default'}
            icon={<ThunderboltOutlined />}
            onClick={() => setUseWebSocket(!useWebSocket)}
            title={wsConnected ? 'WebSocket 已连接' : 'WebSocket 未连接'}
          >
            WebSocket
          </Button>
        </Space>
      </div>

      <div className="message-list">
        {messages.length === 0 && !streamingMessage ? (
          <Empty description="开始新对话" />
        ) : (
          messages.map((msg) => (
            <MessageItem key={msg.id} message={msg} />
          ))
        )}
        {streamingMessage && (
          <MessageItem
            message={{
              id: -1,
              conversationId: currentConversation?.id || 0,
              role: 'assistant',
              content: streamingMessage,
              createdAt: new Date().toISOString(),
            }}
            isStreaming
          />
        )}
        {loading && !streamingMessage && (
          <div className="loading-indicator">
            <Spin />
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      <div className="input-area">
        <TextArea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="输入消息... (Enter发送, Shift+Enter换行)"
          autoSize={{ minRows: 2, maxRows: 6 }}
          disabled={loading}
        />
        <Button
          type="primary"
          icon={<SendOutlined />}
          onClick={handleSend}
          loading={loading}
          disabled={!input.trim()}
        >
          发送
        </Button>
      </div>
    </div>
  )
}

interface MessageItemProps {
  message: Message
  isStreaming?: boolean
}

const MessageItem = ({ message, isStreaming }: MessageItemProps) => {
  const isUser = message.role === 'user'
  const isSystem = message.role === 'system'

  return (
    <div className={`message-item ${isUser ? 'message-user' : isSystem ? 'message-system' : 'message-assistant'}`}>
      {message.thinkingContent && (
        <div className="thinking-block">
          <strong>思考过程:</strong>
          <ReactMarkdown remarkPlugins={[remarkGfm]}>
            {message.thinkingContent}
          </ReactMarkdown>
        </div>
      )}
      <div className="message-content">
        <ReactMarkdown remarkPlugins={[remarkGfm]}>
          {message.content || ''}
        </ReactMarkdown>
        {isStreaming && <span className="cursor-blink">|</span>}
      </div>
    </div>
  )
}

export default Chat