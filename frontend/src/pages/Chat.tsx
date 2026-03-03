import { useState, useRef, useEffect } from 'react'
import { Input, Button, Spin, Empty, Select, Space, Card } from 'antd'
import { SendOutlined, ClearOutlined } from '@ant-design/icons'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { useConversationStore } from '../../stores/conversationStore'
import { useAssistantStore } from '../../stores/assistantStore'
import { conversationApi, openaiApi } from '../../services/api'
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
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    loadAssistants()
  }, [])

  useEffect(() => {
    scrollToBottom()
  }, [messages, streamingMessage])

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
      // Create new conversation with assistant
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

      // Stream response
      setStreamingMessage('')
      const stream = openaiApi.streamChatCompletions({
        model: selectedAssistant?.modelConfig
          ? JSON.parse(selectedAssistant.modelConfig).model || 'gpt-4'
          : 'gpt-4',
        messages: chatMessages,
        stream: true,
      })

      let fullContent = ''
      for await (const chunk of stream) {
        const content = chunk.choices[0]?.delta?.content
        if (content) {
          fullContent += content
          setStreamingMessage(fullContent)
        }
      }

      // Add assistant message
      if (currentConversation && fullContent) {
        const response = await conversationApi.addMessage(currentConversation.id, {
          conversationId: currentConversation.id,
          content: fullContent,
        })
        if (response.success && response.data) {
          addMessage(response.data)
        }
      }

      setStreamingMessage('')
    } catch (error) {
      console.error('Failed to send message:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleClear = () => {
    setCurrentConversation(null)
    setMessages([])
    setSelectedAssistant(null)
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
        </Space>
      </div>

      <div className="message-list">
        {messages.length === 0 && !streamingMessage ? (
          <Empty description="开始新对话" />
        ) : (
          messages.map((message) => (
            <MessageItem key={message.id} message={message} />
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