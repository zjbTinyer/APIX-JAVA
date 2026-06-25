import { useState, useMemo, useRef, useEffect } from 'react'
import { useChatStore } from '../store/chatStore'
import type { Conversation } from '../lib/types'

function getDateGroup(dateStr: string): string {
    const d = new Date(dateStr)
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const yesterday = new Date(today.getTime() - 86400000)
    const weekStart = new Date(today.getTime() - today.getDay() * 86400000)
    const monthStart = new Date(today.getFullYear(), today.getMonth(), 1)

    if (d >= today) return '今天'
    if (d >= yesterday) return '昨天'
    if (d >= weekStart) return '这周内'
    if (d >= monthStart) return '本月'
    return '更早以前'
}

const GROUP_ORDER = ['今天', '昨天', '这周内', '本月', '更早以前']

export default function HistoryPanel() {
    const { currentHistoryId, setCurrentHistoryId, conversations, addConversation, updateConversation, deleteConversation } = useChatStore()
    const [search, setSearch] = useState('')
    const [contextMenu, setContextMenu] = useState<{ x: number; y: number; conv: Conversation } | null>(null)
    const [renaming, setRenaming] = useState<string | null>(null)
    const [renameText, setRenameText] = useState('')
    const renameInputRef = useRef<HTMLInputElement>(null)

    // 搜索过滤
    const filtered = useMemo(() => {
        if (!search.trim()) return conversations
        const q = search.toLowerCase()
        return conversations.filter((c) => c.title.toLowerCase().includes(q))
    }, [conversations, search])

    // 按日期分组
    const grouped = useMemo(() => {
        const map: Record<string, Conversation[]> = {}
        filtered.forEach((c) => {
            const group = getDateGroup(c.updatedAt)
            if (!map[group]) map[group] = []
            map[group].push(c)
        })
        return GROUP_ORDER.filter((g) => map[g]?.length).map((g) => ({ group: g, items: map[g] }))
    }, [filtered])

    const createNewChat = () => {
        const id = `conv_${Date.now()}`
        addConversation({ id, title: '新对话', starred: false, updatedAt: new Date().toISOString(), messageCount: 0 })
        setCurrentHistoryId(id)
    }

    // 右键菜单
    const handleContextMenu = (e: React.MouseEvent, conv: Conversation) => {
        e.preventDefault()
        setContextMenu({ x: e.clientX, y: e.clientY, conv })
    }

    const startRename = (conv: Conversation) => {
        setRenaming(conv.id)
        setRenameText(conv.title)
        setContextMenu(null)
        setTimeout(() => renameInputRef.current?.select(), 50)
    }

    const submitRename = (id: string) => {
        if (renameText.trim()) updateConversation(id, (c) => { c.title = renameText.trim() })
        setRenaming(null)
    }

    const toggleStar = (id: string) => {
        updateConversation(id, (c) => { c.starred = !c.starred })
    }

    // 键盘快捷键
    useEffect(() => {
        const handler = (e: KeyboardEvent) => {
            if ((e.metaKey || e.ctrlKey) && e.key === 'n') { e.preventDefault(); createNewChat() }
        }
        window.addEventListener('keydown', handler)
        return () => window.removeEventListener('keydown', handler)
    }, [])

    // 关闭右键菜单
    useEffect(() => {
        const handler = () => setContextMenu(null)
        window.addEventListener('click', handler)
        return () => window.removeEventListener('click', handler)
    }, [])

    return (
        <aside className="w-64 border-r border-gray-200 bg-white flex flex-col shrink-0 relative">
            {/* 新建对话 + 搜索 */}
            <div className="p-3 border-b border-gray-100 space-y-2 shrink-0">
                <button onClick={createNewChat} className="w-full py-2 px-3 bg-indigo-500 hover:bg-indigo-600 text-white text-sm font-medium rounded-lg transition">
                    + 新对话 <span className="text-indigo-200 ml-1">⌘N</span>
                </button>
                <input
                    type="text"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    placeholder="搜索历史对话..."
                    className="w-full px-3 py-1.5 border border-gray-200 rounded-lg text-xs outline-none focus:ring-1 focus:ring-indigo-500"
                />
            </div>

            {/* 历史列表 */}
            <div className="flex-1 overflow-y-auto p-2 space-y-3">
                {grouped.length === 0 ? (
                    <p className="text-xs text-gray-400 text-center py-8">
                        {search ? '无匹配结果' : '暂无历史对话，开始新对话吧'}
                    </p>
                ) : (
                    grouped.map(({ group, items }) => (
                        <div key={group}>
                            <div className="text-[10px] text-gray-400 font-medium px-2 mb-1 uppercase tracking-wider">{group}</div>
                            <div className="space-y-0.5">
                                {items.map((conv) => (
                                    <div key={conv.id} className="relative group/item">
                                        <button
                                            onClick={() => setCurrentHistoryId(conv.id)}
                                            onContextMenu={(e) => handleContextMenu(e, conv)}
                                            className={`w-full text-left px-3 py-2 rounded-lg text-sm transition flex items-center gap-1 ${currentHistoryId === conv.id
                                                    ? 'bg-indigo-50 text-indigo-700 font-medium'
                                                    : 'text-gray-700 hover:bg-gray-50'
                                                }`}
                                        >
                                            {/* 星标 */}
                                            {conv.starred && <span className="text-amber-400 text-xs shrink-0">★</span>}

                                            {/* 标题 */}
                                            {renaming === conv.id ? (
                                                <input
                                                    ref={renameInputRef}
                                                    value={renameText}
                                                    onChange={(e) => setRenameText(e.target.value)}
                                                    onBlur={() => submitRename(conv.id)}
                                                    onKeyDown={(e) => {
                                                        if (e.key === 'Enter') submitRename(conv.id)
                                                        if (e.key === 'Escape') setRenaming(null)
                                                    }}
                                                    className="flex-1 min-w-0 px-1 py-0.5 border border-indigo-300 rounded text-xs outline-none"
                                                    onClick={(e) => e.stopPropagation()}
                                                    autoFocus
                                                />
                                            ) : (
                                                <span className="flex-1 min-w-0 truncate">{conv.title}</span>
                                            )}
                                        </button>

                                        {/* 星标按钮 */}
                                        <button
                                            onClick={(e) => { e.stopPropagation(); toggleStar(conv.id) }}
                                            className={`absolute right-1 top-1/2 -translate-y-1/2 opacity-0 group-hover/item:opacity-100 transition p-1 rounded hover:bg-gray-200 text-xs ${conv.starred ? 'opacity-100 text-amber-400' : 'text-gray-300'
                                                }`}
                                            title={conv.starred ? '取消星标' : '星标'}
                                        >
                                            {conv.starred ? '★' : '☆'}
                                        </button>
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))
                )}
            </div>

            {/* 右键菜单 */}
            {contextMenu && (
                <div
                    className="fixed z-50 bg-white rounded-xl shadow-xl border border-gray-200 py-1 w-40"
                    style={{ left: contextMenu.x, top: contextMenu.y }}
                    onClick={() => setContextMenu(null)}
                >
                    <button
                        onClick={() => { toggleStar(contextMenu.conv.id); setContextMenu(null) }}
                        className="w-full text-left px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 transition"
                    >
                        {contextMenu.conv.starred ? '取消星标' : '⭐ 星标'}
                    </button>
                    <button
                        onClick={() => { startRename(contextMenu.conv); setContextMenu(null) }}
                        className="w-full text-left px-3 py-2 text-sm text-gray-700 hover:bg-gray-50 transition"
                    >
                        ✏️ 重命名
                    </button>
                    <hr className="my-1 border-gray-100" />
                    <button
                        onClick={() => { deleteConversation(contextMenu.conv.id); setContextMenu(null) }}
                        className="w-full text-left px-3 py-2 text-sm text-red-500 hover:bg-red-50 transition"
                    >
                        🗑 删除
                    </button>
                </div>
            )}
        </aside>
    )
}
