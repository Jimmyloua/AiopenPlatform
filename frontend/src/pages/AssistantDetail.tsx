import { useEffect, useState } from 'react'
import { Card, Descriptions, Tag, Button, Space, message, Spin, Modal, Form, Input, Select } from 'antd'
import { useParams, useNavigate } from 'react-router-dom'
import { SendOutlined, EditOutlined, CheckOutlined, CloseOutlined } from '@ant-design/icons'
import { assistantApi } from '../../services/api'
import { Assistant } from '../../types'
import { useUserStore } from '../../stores/userStore'

const AssistantDetail = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useUserStore()
  const [assistant, setAssistant] = useState<Assistant | null>(null)
  const [loading, setLoading] = useState(true)
  const [editModalVisible, setEditModalVisible] = useState(false)
  const [form] = Form.useForm()

  useEffect(() => {
    if (id) {
      loadAssistant()
    }
  }, [id])

  const loadAssistant = async () => {
    setLoading(true)
    try {
      const response = await assistantApi.get(Number(id))
      if (response.success && response.data) {
        setAssistant(response.data)
      }
    } catch (error) {
      message.error('加载助手信息失败')
    } finally {
      setLoading(false)
    }
  }

  const handleChat = () => {
    navigate(`/chat?assistant=${id}`)
  }

  const handleEdit = () => {
    if (assistant) {
      form.setFieldsValue({
        name: assistant.name,
        description: assistant.description,
        systemPrompt: assistant.systemPrompt,
        capabilities: assistant.capabilities,
      })
      setEditModalVisible(true)
    }
  }

  const handleUpdate = async (values: any) => {
    try {
      const response = await assistantApi.update(Number(id), values)
      if (response.success && response.data) {
        message.success('更新成功')
        setAssistant(response.data)
        setEditModalVisible(false)
      }
    } catch (error) {
      message.error('更新失败')
    }
  }

  const handlePublish = async () => {
    try {
      const response = await assistantApi.publish(Number(id))
      if (response.success && response.data) {
        message.success('已提交审核')
        setAssistant(response.data)
      }
    } catch (error) {
      message.error('发布失败')
    }
  }

  const handleApprove = async () => {
    try {
      const response = await assistantApi.approve(Number(id))
      if (response.success && response.data) {
        message.success('已通过审核')
        setAssistant(response.data)
      }
    } catch (error) {
      message.error('审核失败')
    }
  }

  const handleReject = async () => {
    Modal.confirm({
      title: '拒绝原因',
      content: (
        <Input.TextArea id="rejectReason" rows={3} placeholder="请输入拒绝原因" />
      ),
      onOk: async () => {
        const reason = (document.getElementById('rejectReason') as HTMLTextAreaElement)?.value
        try {
          const response = await assistantApi.reject(Number(id), reason)
          if (response.success && response.data) {
            message.success('已拒绝')
            setAssistant(response.data)
          }
        } catch (error) {
          message.error('操作失败')
        }
      },
    })
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

  if (loading) {
    return <Spin />
  }

  if (!assistant) {
    return <div>助手不存在</div>
  }

  const isOwner = assistant.createdBy?.id === user?.id
  const isAdmin = user?.role === 'admin'

  return (
    <div>
      <Card
        title={
          <Space>
            {assistant.name}
            {getStatusTag(assistant.status)}
          </Space>
        }
        extra={
          <Space>
            <Button type="primary" icon={<SendOutlined />} onClick={handleChat}>
              开始对话
            </Button>
            {isOwner && (
              <>
                <Button icon={<EditOutlined />} onClick={handleEdit}>
                  编辑
                </Button>
                {assistant.status === 'draft' && (
                  <Button onClick={handlePublish}>发布到广场</Button>
                )}
              </>
            )}
            {isAdmin && assistant.status === 'pending_review' && (
              <>
                <Button type="primary" icon={<CheckOutlined />} onClick={handleApprove}>
                  通过
                </Button>
                <Button danger icon={<CloseOutlined />} onClick={handleReject}>
                  拒绝
                </Button>
              </>
            )}
          </Space>
        }
      >
        <Descriptions column={2}>
          <Descriptions.Item label="名称">{assistant.name}</Descriptions.Item>
          <Descriptions.Item label="状态">{getStatusTag(assistant.status)}</Descriptions.Item>
          <Descriptions.Item label="描述" span={2}>
            {assistant.description || '暂无描述'}
          </Descriptions.Item>
          <Descriptions.Item label="创建者">
            {assistant.createdBy?.nickname || assistant.createdBy?.username}
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {new Date(assistant.createdAt).toLocaleString()}
          </Descriptions.Item>
          <Descriptions.Item label="能力标签" span={2}>
            <Space>
              {assistant.capabilities?.map((cap, index) => (
                <Tag key={index} color="blue">
                  {cap}
                </Tag>
              ))}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="系统提示词" span={2}>
            <pre style={{ whiteSpace: 'pre-wrap', background: '#f6f6f6', padding: 12, borderRadius: 6 }}>
              {assistant.systemPrompt || '未设置'}
            </pre>
          </Descriptions.Item>
          {assistant.reviewComment && (
            <Descriptions.Item label="审核意见" span={2}>
              {assistant.reviewComment}
            </Descriptions.Item>
          )}
        </Descriptions>
      </Card>

      <Modal
        title="编辑助手"
        open={editModalVisible}
        onCancel={() => setEditModalVisible(false)}
        footer={null}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleUpdate}>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="systemPrompt" label="系统提示词">
            <Input.TextArea rows={4} />
          </Form.Item>
          <Form.Item name="capabilities" label="能力标签">
            <Select mode="tags" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                保存
              </Button>
              <Button onClick={() => setEditModalVisible(false)}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default AssistantDetail