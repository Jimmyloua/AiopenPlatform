import { useState } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { Layout, Menu, Avatar, Dropdown, Button, Space } from 'antd'
import {
  MessageOutlined,
  AppstoreOutlined,
  SettingOutlined,
  UserOutlined,
  LogoutOutlined,
  PlusOutlined,
} from '@ant-design/icons'
import { useUserStore } from '../../stores/userStore'
import { useConversationStore } from '../../stores/conversationStore'
import './MainLayout.css'

const { Sider, Header, Content } = Layout

const MainLayout = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useUserStore()
  const { conversations } = useConversationStore()
  const [collapsed, setCollapsed] = useState(false)

  const menuItems = [
    {
      key: '/chat',
      icon: <MessageOutlined />,
      label: '对话',
    },
    {
      key: '/assistants',
      icon: <AppstoreOutlined />,
      label: '助手广场',
    },
    {
      key: '/settings',
      icon: <SettingOutlined />,
      label: '设置',
    },
  ]

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人信息',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
    },
  ]

  const handleMenuClick = (key: string) => {
    navigate(key)
  }

  const handleUserMenuClick = (key: string) => {
    if (key === 'logout') {
      logout()
      navigate('/login')
    } else if (key === 'profile') {
      navigate('/settings')
    }
  }

  const handleNewChat = () => {
    navigate('/chat')
  }

  return (
    <Layout className="main-layout">
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        className="sidebar"
        theme="dark"
      >
        <div className="sidebar-header">
          <h1>{collapsed ? 'AI' : 'AI开放平台'}</h1>
        </div>

        <div className="new-chat-btn">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            block
            onClick={handleNewChat}
          >
            {!collapsed && '新对话'}
          </Button>
        </div>

        <Menu
          theme="dark"
          selectedKeys={[location.pathname]}
          mode="inline"
          items={menuItems}
          onClick={({ key }) => handleMenuClick(key)}
        />

        {!collapsed && conversations.length > 0 && (
          <div className="recent-conversations">
            <h4>最近对话</h4>
            <ul>
              {conversations.slice(0, 5).map((conv) => (
                <li key={conv.id} onClick={() => navigate(`/chat/${conv.id}`)}>
                  {conv.title || '新对话'}
                </li>
              ))}
            </ul>
          </div>
        )}
      </Sider>

      <Layout>
        <Header className="header">
          <div className="header-title">
            {menuItems.find((item) => item.key === location.pathname)?.label || 'AI开放平台'}
          </div>

          <Dropdown
            menu={{
              items: userMenuItems,
              onClick: ({ key }) => handleUserMenuClick(key),
            }}
            placement="bottomRight"
          >
            <Space className="user-info">
              <Avatar icon={<UserOutlined />} src={user?.avatar} />
              <span>{user?.nickname || user?.username}</span>
            </Space>
          </Dropdown>
        </Header>

        <Content className="content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default MainLayout