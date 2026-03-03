import { Card, Tabs, Form, Input, Button, message, Avatar, Upload, Descriptions } from 'antd'
import { UserOutlined, UploadOutlined } from '@ant-design/icons'
import { useUserStore } from '../../stores/userStore'
import { authApi } from '../../services/api'
import { useState } from 'react'

const Settings = () => {
  const { user, fetchUser } = useUserStore()
  const [loading, setLoading] = useState(false)
  const [profileForm] = Form.useForm()
  const [passwordForm] = Form.useForm()

  const handleUpdateProfile = async (values: any) => {
    setLoading(true)
    try {
      // API call would go here
      message.success('更新成功')
      fetchUser()
    } catch (error) {
      message.error('更新失败')
    } finally {
      setLoading(false)
    }
  }

  const handleChangePassword = async (values: any) => {
    if (values.newPassword !== values.confirmPassword) {
      message.error('两次密码输入不一致')
      return
    }
    setLoading(true)
    try {
      // API call would go here
      message.success('密码修改成功')
      passwordForm.resetFields()
    } catch (error) {
      message.error('密码修改失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="settings-page">
      <Tabs defaultActiveKey="profile" tabPosition="left">
        <Tabs.TabPane tab="个人资料" key="profile">
          <Card title="个人资料">
            <div style={{ display: 'flex', gap: 48 }}>
              <div style={{ textAlign: 'center' }}>
                <Avatar size={100} icon={<UserOutlined />} src={user?.avatar} />
                <div style={{ marginTop: 16 }}>
                  <Upload showUploadList={false}>
                    <Button icon={<UploadOutlined />}>更换头像</Button>
                  </Upload>
                </div>
              </div>
              <div style={{ flex: 1 }}>
                <Form
                  form={profileForm}
                  layout="vertical"
                  initialValues={{
                    nickname: user?.nickname,
                    email: user?.email,
                  }}
                  onFinish={handleUpdateProfile}
                >
                  <Form.Item name="nickname" label="昵称">
                    <Input placeholder="输入昵称" />
                  </Form.Item>
                  <Form.Item name="email" label="邮箱">
                    <Input placeholder="输入邮箱" disabled />
                  </Form.Item>
                  <Form.Item>
                    <Button type="primary" htmlType="submit" loading={loading}>
                      保存修改
                    </Button>
                  </Form.Item>
                </Form>
              </div>
            </div>
          </Card>
        </Tabs.TabPane>

        <Tabs.TabPane tab="账号安全" key="security">
          <Card title="修改密码">
            <Form form={passwordForm} layout="vertical" onFinish={handleChangePassword}>
              <Form.Item
                name="oldPassword"
                label="当前密码"
                rules={[{ required: true, message: '请输入当前密码' }]}
              >
                <Input.Password placeholder="输入当前密码" />
              </Form.Item>
              <Form.Item
                name="newPassword"
                label="新密码"
                rules={[
                  { required: true, message: '请输入新密码' },
                  { min: 6, message: '密码至少6位' },
                ]}
              >
                <Input.Password placeholder="输入新密码" />
              </Form.Item>
              <Form.Item
                name="confirmPassword"
                label="确认密码"
                rules={[
                  { required: true, message: '请确认新密码' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue('newPassword') === value) {
                        return Promise.resolve()
                      }
                      return Promise.reject(new Error('两次密码输入不一致'))
                    },
                  }),
                ]}
              >
                <Input.Password placeholder="再次输入新密码" />
              </Form.Item>
              <Form.Item>
                <Button type="primary" htmlType="submit" loading={loading}>
                  修改密码
                </Button>
              </Form.Item>
            </Form>
          </Card>
        </Tabs.TabPane>

        <Tabs.TabPane tab="API密钥" key="apiKey">
          <Card title="API密钥管理">
            <Descriptions>
              <Descriptions.Item label="API Key">暂无API密钥</Descriptions.Item>
            </Descriptions>
            <Button type="primary" style={{ marginTop: 16 }}>
              生成新密钥
            </Button>
          </Card>
        </Tabs.TabPane>

        <Tabs.TabPane tab="关于" key="about">
          <Card title="关于系统">
            <Descriptions column={1}>
              <Descriptions.Item label="系统名称">AI开放平台</Descriptions.Item>
              <Descriptions.Item label="版本">1.0.0</Descriptions.Item>
              <Descriptions.Item label="描述">
                一个支持多租户的AI助手平台，提供助手管理、对话管理、技能配置等功能
              </Descriptions.Item>
            </Descriptions>
          </Card>
        </Tabs.TabPane>
      </Tabs>
    </div>
  )
}

export default Settings