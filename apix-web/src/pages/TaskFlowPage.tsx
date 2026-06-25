import { useState } from 'react'
import FileExplorer from '../components/file/FileExplorer'
import CodeEditor from '../components/editor/CodeEditor'
import MiniChatPanel from '../components/mini/MiniChatPanel'
import { useChatStore } from '../store/chatStore'

interface Tab {
    key: string
    title: string
    type: 'aflow' | 'md' | 'py' | 'js' | 'txt'
    content: string
}

const TASK_CARDS = [
    { id: 'start', title: '开始' },
    { id: 'llm', title: 'LLM 调用' },
    { id: 'code', title: '代码执行' },
    { id: 'search', title: '网络搜索' },
    { id: 'file', title: '文件操作' },
    { id: 'condition', title: '条件判断' },
    { id: 'output', title: '输出' },
]

export default function TaskFlowPage() {
    const [tabs, setTabs] = useState<Tab[]>([])
    const [activeTab, setActiveTab] = useState<string>('')
    const [showFlowPanel, setShowFlowPanel] = useState(true)
    const [showMiniChat, setShowMiniChat] = useState(false)

    const config = useChatStore((s) => s.config)
    const activeTabMeta = tabs.find((t) => t.key === activeTab)

    const openFile = (path: string) => {
        const ext = path.split('.').pop() || 'txt'
        const typeMap: Record<string, Tab['type']> = {
            md: 'md', py: 'py', js: 'js', ts: 'txt', json: 'txt', html: 'txt', css: 'txt',
        }
        const tabType = typeMap[ext] || 'txt'
        const name = path.split('/').pop() || path
        if (!tabs.find((t) => t.key === path)) {
            setTabs([...tabs, { key: path, title: name, type: tabType, content: `// ${path}\n\n` }])
        }
        setActiveTab(path)
    }

    const closeTab = (key: string) => {
        const newTabs = tabs.filter((t) => t.key !== key)
        setTabs(newTabs)
        if (activeTab === key) setActiveTab(newTabs[newTabs.length - 1]?.key || '')
    }

    const updateContent = (content: string) => {
        setTabs(tabs.map((t) => t.key === activeTab ? { ...t, content } : t))
    }

    return (
        <div className="h-full flex relative">
            <FileExplorer onOpenFile={openFile} />

            {showFlowPanel && (
                <div className="w-48 bg-gray-50 border-r border-gray-200 flex flex-col shrink-0">
                    <div className="px-3 py-2 border-b border-gray-200 text-sm font-medium text-gray-700 shrink-0">任务卡</div>
                    <div className="flex-1 overflow-y-auto p-2 space-y-1">
                        {TASK_CARDS.map((card) => (
                            <div key={card.id} draggable
                                className="px-3 py-2 bg-white border border-gray-200 rounded-lg text-sm text-gray-700 cursor-grab active:cursor-grabbing hover:shadow-md transition-shadow"
                                onDragStart={(e) => e.dataTransfer.setData('text/plain', card.id)}
                            >{card.title}</div>
                        ))}
                    </div>
                </div>
            )}

            {/* 编辑器 */}
            <div className="flex-1 flex flex-col bg-white min-w-0">
                <div className="flex items-center border-b border-gray-200 bg-gray-50 overflow-x-auto shrink-0">
                    {tabs.map((tab) => (
                        <div key={tab.key} onClick={() => setActiveTab(tab.key)}
                            className={`flex items-center gap-1 px-3 py-2 text-sm cursor-pointer border-r border-gray-200 whitespace-nowrap ${activeTab === tab.key ? 'bg-white text-indigo-600 font-medium' : 'text-gray-500 hover:bg-gray-100'}`}>
                            <span>{tab.title}</span>
                            <button onClick={(e) => { e.stopPropagation(); closeTab(tab.key) }} className="text-gray-300 hover:text-gray-600 ml-1 text-xs">✕</button>
                        </div>
                    ))}
                    {tabs.length === 0 && <div className="px-3 py-2 text-sm text-gray-400">打开文件开始编辑</div>}
                    <div className="flex-1" />
                    <button
                        onClick={() => setShowMiniChat(!showMiniChat)}
                        className={`px-3 py-2 text-xs border-l border-gray-200 transition ${showMiniChat ? 'bg-indigo-50 text-indigo-600' : 'text-gray-400 hover:text-gray-600'}`}
                    >
                        💬 Mini Chat
                    </button>
                </div>

                <div className="flex-1 flex min-h-0">
                    {activeTabMeta ? (
                        <div className="flex-1">
                            {activeTabMeta.type === 'aflow' ? (
                                <div className="h-full flex items-center justify-center bg-gray-50 text-gray-400">
                                    <div className="text-center">
                                        <div className="text-4xl mb-3">🔀</div>
                                        <p className="text-sm">可视化流程图编排</p>
                                        <p className="text-xs mt-1">从左侧拖拽任务卡到此处</p>
                                    </div>
                                </div>
                            ) : (
                                <CodeEditor value={activeTabMeta.content} onChange={updateContent} language={activeTabMeta.type} fileName={activeTabMeta.title} />
                            )}
                        </div>
                    ) : (
                        <div className="flex-1 flex items-center justify-center bg-gray-50 text-gray-400">
                            <div className="text-center">
                                <div className="text-5xl mb-4">📝</div>
                                <p className="text-sm">选择左侧文件或拖拽任务卡开始工作</p>
                                {config.workDir && <p className="text-xs mt-2 text-gray-300">工作区: {config.workDir}</p>}
                            </div>
                        </div>
                    )}

                    {/* Mini Chat */}
                    {showMiniChat && (
                        <MiniChatPanel
                            pageId="flow"
                            activedFilePath={activeTabMeta?.key}
                            activedFileName={activeTabMeta?.title}
                            onClose={() => setShowMiniChat(false)}
                        />
                    )}
                </div>
            </div>

            {/* 切换左侧面板按钮 */}
            <button onClick={() => setShowFlowPanel(!showFlowPanel)}
                className="absolute left-0 top-1/2 -translate-y-1/2 bg-white border border-gray-200 rounded-r-md px-1 py-4 text-xs text-gray-400 hover:text-gray-600 z-10"
                style={{ marginLeft: showFlowPanel ? '48px' : '0' }}
            >{showFlowPanel ? '◀' : '▶'}</button>
        </div>
    )
}
