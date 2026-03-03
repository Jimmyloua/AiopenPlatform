import { useState, useEffect } from 'react'
import { Row, Col, Card, Tag, Button, Input, Select, Space, Modal, Form, message, Spin } from 'antd'
import { PlusOutlined, EyeOutlined, EditOutlined, DeleteOutlined, SendOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useAssistantStore } from '../../stores/assistantStore'
import { assistantApi } from '../../services/api'
import { Assistant, AssistantCreateRequest } from '../../types'
import './AssistantPlaza.css'

const { Search } = Input

const AssistantPlaza = () => {
  const navigate = useNavigate()
  const { assistants, setPageInfo, setAssistants, removeAssistant } = useAssistantStore()
  const [loading, setLoading] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [category, setCategory] = useState<string>()
  const [createModalVisible, setCreateModalVisible] = useState(false)
  const [form] = Form.useForm()

  useEffect(() => {
    loadAssistants()
  }, [category])

  const loadAssistants = async (page = 1) => {
    setLoading(true)
    try {
      const response = await assistantApi.list(page, 12, true)
      if (response.success && response.data) {
        setPageInfo(response.data)
      }
    } catch (error) {
      message.error('加载助手列表失败')
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = (value: string) => {
    setSearchText(value)
    const filtered = assistants.filter(
      (a) =>
        a.name.toLowerCase().includes(value.toLowerCase()) ||
        a.description?.toLowerCase().includes(value.toLowerCase())
    )
    setAssistants(filtered)
  }

  const handleCreate = async (values: AssistantCreateRequest) => {
    try {
      const response = await assistantApi.create(values)
      if (response.success && response.data) {
        message.success('创建成功')
        setCreateModalVisible(false)
        form.resetFields()
        loadAssistants()
      }
    } catch (error) {
      message.error('创建失败')
    }
  }

  const handleDelete = async (id: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个助手吗？',
      onOk: async () => {
        try {
          const response = await assistantApi.delete(id)
          if (response.success) {
            message.success('删除成功')
            removeAssistant(id)
          }
        } catch (error) {
          message.error('删除失败')
        }
      },
    })
  }

  const handlePublish = async (id: number) => {
    try {
      const response = await assistantApi.publish(id)
      if (response.success) {
        message.success('已提交审核')
        loadAssistants()
      }
    } catch (error) {
      message.error('发布失败')
    }
  }

  const handleChat = (assistant: Assistant) => {
    navigate(`/chat?assistant=${assistant.id}`)
  }

  const getStatusTag = (status: string) => {
    const statusMap: Record<string, { color: string; text: string }> = {
      draft: { color: 'default', text: '草稿' },
      pending_review: { color: 'processing', text: '待审核' },
      published: { color: 'success', text: '已发布' },
      rejected: { color: 'error', text: '已拒绝' },
    }
    const config = statusMap[status] || { color: 'default', text: status }
    return <Tag color={config.color}>{config.text}</Tag>
  }

  return (
    <div className="assistant-plaza">
      <div className="plaza-header">
        <h2>助手广场</h2>
        <Space>
          <Search
            placeholder="搜索助手"
            onSearch={handleSearch}
            style={{ width: 200 }}
          />
          <Select
            placeholder="选择分类"
            allowClear
            style={{ width: 150 }}
            onChange={setCategory}
            options={[
              { value: 'general', label: '通用' },
              { value: 'coding', label: '编程' },
              { value: 'writing', label: '写作' },
              { value: 'analysis', label: '分析' },
            ]}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateModalVisible(true)}>
            创建助手
          </Button>
        </Space>
      </div>

      <Spin spinning={loading}>
        <Row gutter={[16, 16]}>
          {assistants.map((assistant) => (
            <Col xs={24} sm={12} md={8} lg={6} key={assistant.id}>
              <Card
                className="assistant-card"
                cover={
                  assistant.avatar ? (
                    <img alt={assistant.name} src={assistant.avatar} />
                  ) : (
                    <div className="assistant-avatar-placeholder">
                      {assistant.name[0]}
                    </div>
                  )
                }
                actions={[
                  <EyeOutlined key="view" onClick={() => navigate(`/assistants/${assistant.id}`)} />,
                  <SendOutlined key="chat" onClick={() => handleChat(assistant)} />,
                  <DeleteOutlined key="delete" onClick={() => handleDelete(assistant.id)} />,
                ]}
              >
                <Card.Meta
                  title={
                    <Space>
                      {assistant.name}
                      {getStatusTag(assistant.status)}
                    </Space>
                  }
                  description={assistant.description || '暂无描述'}
                />
                <div className="assistant-capabilities">
                  {assistant.capabilities?.slice(0, 3).map((cap, index) => (
                    <Tag key={index} color="blue">
                      {cap}
                    </Tag>
                  ))}
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      </Spin>

      <Modal
        title="创建助手"
        open={createModalVisible}
        onCancel={() => setCreateModalVisible(false)}
        footer={null}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入助手名称' }]}
          >
            <Input placeholder="输入助手名称" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="描述助手的功能和特点" />
          </Form.Item>
          <Form.Item name="systemPrompt" label="系统提示词">
            <Input.TextArea rows={4} placeholder="定义助手的行为和角色" />
          </Form.Item>
          <Form.Item name="capabilities" label="能力标签">
            <Select
              mode="tags"
              placeholder="输入能力标签"
              options={[
                { value: '对话', label: '对话' },
                { value: '编程', label: '编程' },
                { value: '写作', label: '写作' },
                { value: '分析', label: '分析' },
              ]}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                创建
              </Button>
              <Button onClick={() => setCreateModalVisible(false)}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default AssistantPlaza