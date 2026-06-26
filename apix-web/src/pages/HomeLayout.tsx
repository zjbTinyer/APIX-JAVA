import { useState, useEffect } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { useChatStore } from '../store/chatStore'

interface NavItem {
    path: string
    label: string
    icon: (active: boolean) => React.ReactNode
}

const navItems: NavItem[] = [
    {
        path: '/chat', label: '智能体',
        icon: (a) => <svg className={`w-5 h-5 ${a ? 'text-indigo-400' : ''}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>,
    },
    {
        path: '/data', label: '数据中心',
        icon: (a) => <svg className={`w-5 h-5 ${a ? 'text-indigo-400' : ''}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><ellipse cx="12" cy="5" rx="9" ry="3" /><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3" /><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5" /></svg>,
    },
    {
        path: '/tasks', label: '任务管理',
        icon: (a) => <svg className={`w-5 h-5 ${a ? 'text-indigo-400' : ''}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M9 11l3 3L22 4" /><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" /></svg>,
    },
    {
        path: '/flow', label: '工作流',
        icon: (a) => <svg className={`w-5 h-5 ${a ? 'text-indigo-400' : ''}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="16 18 22 12 16 6" /><polyline points="8 6 2 12 8 18" /></svg>,
    },
    {
        path: '/reports', label: '报告',
        icon: (a) => <svg className={`w-5 h-5 ${a ? 'text-indigo-400' : ''}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="20" x2="18" y2="10" /><line x1="12" y1="20" x2="12" y2="4" /><line x1="6" y1="20" x2="6" y2="14" /></svg>,
    },
    {
        path: '/servers', label: '服务',
        icon: (a) => <svg className={`w-5 h-5 ${a ? 'text-indigo-400' : ''}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="2" y="2" width="20" height="8" rx="2" ry="2" /><rect x="2" y="14" width="20" height="8" rx="2" ry="2" /><line x1="6" y1="6" x2="6.01" y2="6" /><line x1="6" y1="18" x2="6.01" y2="18" /></svg>,
    },
    {
        path: '/settings', label: '设置',
        icon: (a) => <svg className={`w-5 h-5 ${a ? 'text-indigo-400' : ''}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" /></svg>,
    },
]

export default function HomeLayout() {
    const navigate = useNavigate()
    const location = useLocation()
    const { user, logout } = useAuthStore()
    const { config } = useChatStore()
    const [collapsed, setCollapsed] = useState(false)

    // 应用深色主题
    useEffect(() => {
        if (config.darkTheme) {
            document.documentElement.classList.add('dark')
        } else {
            document.documentElement.classList.remove('dark')
        }
    }, [config.darkTheme])

    const handleLogout = () => {
        logout()
        navigate('/login', { replace: true })
    }

    const isActive = (path: string) => location.pathname.startsWith(path)
    const sidebarWidth = collapsed ? 'w-14' : 'w-16'

    return (
        <div className="h-screen flex bg-[var(--apix-bg)]">
            {/* Sidebar */}
            <aside className={`${sidebarWidth} bg-[var(--apix-sidebar)] flex flex-col items-center py-4 gap-1 shrink-0 transition-all duration-300 ease-[cubic-bezier(0.16,1,0.3,1)] relative z-20`}>
                {/* Logo */}
                <div
                    onClick={() => navigate('/chat')}
                    className="w-10 h-10 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-xl flex items-center justify-center text-white font-bold text-sm mb-4 cursor-pointer shadow-lg hover:shadow-indigo-500/25 transition-shadow duration-200"
                >
                    <svg className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                        <polygon points="12 2 22 8.5 22 15.5 12 22 2 15.5 2 8.5 12 2" />
                        <line x1="12" y1="22" x2="12" y2="15.5" />
                        <polyline points="22 8.5 12 15.5 2 8.5" />
                    </svg>
                </div>

                {/* 导航项 */}
                {navItems.map((item) => {
                    const active = isActive(item.path)
                    return (
                        <div key={item.path} className="relative group">
                            <button
                                onClick={() => navigate(item.path)}
                                className={`w-10 h-10 flex items-center justify-center rounded-xl transition-all duration-200 ${active
                                        ? 'bg-indigo-500/20 text-indigo-400 shadow-sm'
                                        : 'text-gray-400 hover:bg-[var(--apix-sidebar-hover)] hover:text-gray-200'
                                    }`}
                            >
                                {item.icon(active)}
                            </button>
                            {/* Tooltip (折叠时显示) */}
                            <div className="absolute left-full ml-2 top-1/2 -translate-y-1/2 px-2.5 py-1.5 bg-gray-900 text-white text-xs rounded-lg whitespace-nowrap opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 pointer-events-none z-50 shadow-lg">
                                {item.label}
                            </div>
                        </div>
                    )
                })}

                <div className="flex-1" />

                {/* 折叠按钮 */}
                <button
                    onClick={() => setCollapsed(!collapsed)}
                    className="w-10 h-8 flex items-center justify-center rounded-lg text-gray-500 hover:bg-[var(--apix-sidebar-hover)] hover:text-gray-300 transition-all duration-200 text-xs"
                    title={collapsed ? '展开侧边栏' : '折叠侧边栏'}
                >
                    <svg className={`w-3.5 h-3.5 transition-transform duration-300 ${collapsed ? '' : 'rotate-180'}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                        <polyline points="15 18 9 12 15 6" />
                    </svg>
                </button>

                {/* 退出登录 */}
                <div className="relative group">
                    <button
                        onClick={handleLogout}
                        className="w-10 h-10 flex items-center justify-center rounded-xl text-gray-400 hover:bg-red-500/10 hover:text-red-400 transition-all duration-200"
                    >
                        <svg className="w-4.5 h-4.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                            <polyline points="16 17 21 12 16 7" />
                            <line x1="21" y1="12" x2="9" y2="12" />
                        </svg>
                    </button>
                    <div className="absolute left-full ml-2 top-1/2 -translate-y-1/2 px-2.5 py-1.5 bg-gray-900 text-white text-xs rounded-lg whitespace-nowrap opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 pointer-events-none z-50 shadow-lg">
                        退出登录
                    </div>
                </div>
            </aside>

            {/* Main */}
            <main className="flex-1 flex flex-col overflow-hidden">
                {user && (
                    <header className="h-12 border-b border-[var(--apix-border)] bg-[var(--apix-bg-card)] flex items-center justify-between px-6 shrink-0 transition-theme">
                        <span className="text-sm text-[var(--apix-text-secondary)]">
                            欢迎, <span className="font-medium text-[var(--apix-text)]">{user.username}</span>
                        </span>
                        <span className="text-xs text-[var(--apix-text-tertiary)]">APIX v1.0.0</span>
                    </header>
                )}
                <div className="flex-1 overflow-hidden">
                    <Outlet />
                </div>
            </main>
        </div>
    )
}
