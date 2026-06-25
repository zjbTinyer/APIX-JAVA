import { useState } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

const navItems = [
    { path: '/chat', label: '智能体', icon: '💬' },
    { path: '/data', label: '数据中心', icon: '📊' },
    { path: '/tasks', label: '任务管理', icon: '📋' },
    { path: '/flow', label: '工作流', icon: '🔧' },
    { path: '/reports', label: '报告', icon: '📈' },
    { path: '/servers', label: '服务', icon: '🖥️' },
    { path: '/settings', label: '设置', icon: '⚙️' },
]

export default function HomeLayout() {
    const navigate = useNavigate()
    const location = useLocation()
    const { user, logout } = useAuthStore()
    const [collapsed, setCollapsed] = useState(false)

    const handleLogout = () => {
        logout()
        navigate('/login', { replace: true })
    }

    return (
        <div className="h-screen flex">
            {/* Sidebar */}
            <aside className={`${collapsed ? 'w-14' : 'w-16'} bg-gray-900 flex flex-col items-center py-4 gap-2 shrink-0 transition-all duration-200 relative`}>
                <div className="w-10 h-10 bg-indigo-500 rounded-xl flex items-center justify-center text-white font-bold text-sm mb-4 cursor-pointer" onClick={() => navigate('/chat')}>
                    A
                </div>
                {navItems.map((item) => (
                    <button
                        key={item.path}
                        onClick={() => navigate(item.path)}
                        className={`w-10 h-10 flex items-center justify-center rounded-xl text-lg transition ${location.pathname.startsWith(item.path)
                            ? 'bg-indigo-500/20 text-indigo-400'
                            : 'text-gray-400 hover:bg-gray-800 hover:text-gray-200'
                            }`}
                        title={item.label}
                    >
                        {item.icon}
                    </button>
                ))}
                <div className="flex-1" />
                <button
                    onClick={() => setCollapsed(!collapsed)}
                    className="w-10 h-6 flex items-center justify-center rounded text-gray-500 hover:bg-gray-800 hover:text-gray-300 transition text-xs mb-1"
                    title={collapsed ? '展开' : '折叠'}
                >
                    {collapsed ? '▶' : '◀'}
                </button>
                <button
                    onClick={handleLogout}
                    className="w-10 h-10 flex items-center justify-center rounded-xl text-gray-400 hover:bg-gray-800 hover:text-gray-200 transition"
                    title="退出登录"
                >
                    🚪
                </button>
            </aside>

            {/* Main */}
            <main className="flex-1 flex flex-col overflow-hidden">
                {user && (
                    <header className="h-12 border-b border-gray-200 bg-white flex items-center px-6 shrink-0">
                        <span className="text-sm text-gray-500">
                            欢迎, <span className="font-medium text-gray-700">{user.username}</span>
                        </span>
                    </header>
                )}
                <div className="flex-1 overflow-hidden">
                    <Outlet />
                </div>
            </main>
        </div>
    )
}
