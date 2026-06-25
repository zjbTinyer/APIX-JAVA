import { useState } from 'react'
import ProviderPage from '../components/provider/ProviderPage'
import RagPage from '../components/rag/RagPage'
import SkillPage from '../components/skill/SkillPage'
import RolePage from '../components/role/RolePage'
import McpPage from '../components/mcp/McpPage'

const menuItems = [
    { key: 'provider', label: '供应商', icon: '🔗' },
    { key: 'rag', label: '知识库', icon: '📚' },
    { key: 'skill', label: '技能包', icon: '📦' },
    { key: 'role', label: '角色卡', icon: '👤' },
    { key: 'mcp', label: 'M C P', icon: '🔌' },
]

export default function DataPage() {
    const [activeKey, setActiveKey] = useState('provider')

    const renderContent = () => {
        switch (activeKey) {
            case 'provider': return <ProviderPage />
            case 'rag': return <RagPage />
            case 'skill': return <SkillPage />
            case 'role': return <RolePage />
            case 'mcp': return <McpPage />
            default: return null
        }
    }

    return (
        <div className="h-full flex">
            {/* 左侧菜单 */}
            <div className="w-48 border-r border-gray-200 bg-white shrink-0 p-2 space-y-1">
                {menuItems.map((item) => (
                    <button
                        key={item.key}
                        onClick={() => setActiveKey(item.key)}
                        className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition ${activeKey === item.key
                                ? 'bg-indigo-50 text-indigo-700 font-medium'
                                : 'text-gray-600 hover:bg-gray-50'
                            }`}
                    >
                        <span>{item.icon}</span>
                        <span>{item.label}</span>
                    </button>
                ))}
            </div>

            {/* 右侧内容 */}
            <div className="flex-1 overflow-y-auto bg-gray-50">
                {renderContent()}
            </div>
        </div>
    )
}
