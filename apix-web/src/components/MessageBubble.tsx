import { useState } from 'react'
import type { ChatMessage } from '../lib/types'
import type { ToolCallInfo } from '../lib/types'
import { useChatStore } from '../store/chatStore'
import ContextMenu from './ContextMenu'
import ToolDetailDialog from './ToolDetailDialog'

interface Props {
    message: ChatMessage
    onEditResend?: (msgId: string, newContent: string) => void
    onRegenerate?: (msgId: string) => void
    onBranchNavigate?: (msgId: string, direction: 'pre' | 'next') => void
    onQuote?: (text: string) => void
}

export default function MessageBubble({ message, onEditResend, onRegenerate, onBranchNavigate, onQuote }: Props) {
    const isHuman = message.role === 'human'
    const { selectMode, currentHistoryId, updateMessage, deleteMessages, config } = useChatStore()
    const [editing, setEditing] = useState(false)
    const [editText, setEditText] = useState('')
    const [showActions, setShowActions] = useState(false)
    const [contextMenu, setContextMenu] = useState<{ x: number; y: number } | null>(null)
    const [detailTool, setDetailTool] = useState<ToolCallInfo | null>(null)
    const [previewImg, setPreviewImg] = useState<string | null>(null)

    const startEdit = () => {
        setEditText(message.chunks?.map((c) => c.content).join('') || '')
        setEditing(true)
    }

    const submitEdit = () => {
        if (editText.trim()) onEditResend?.(message.id, editText)
        setEditing(false)
    }

    // 思考链
    const renderThink = () => {
        const parts = message.chunks?.filter((c) => c.label_type === 'think') || []
        if (parts.length === 0) return null
        return (
            <details className="mb-2">
                <summary className="text-xs text-gray-400 cursor-pointer hover:text-gray-600 select-none">{message.label || '思考过程'}</summary>
                <div className="mt-1 text-sm text-gray-500 italic border-l-2 border-gray-300 pl-3 whitespace-pre-wrap">
                    {parts.map((c, i) => <span key={i}>{c.content}</span>)}
                </div>
            </details>
        )
    }

    // 内容
    const renderContent = () => {
        const parts = message.chunks?.filter((c) => c.label_type === 'content') || []
        if (parts.length === 0 && !message.pending) return <p className="text-gray-400 italic">(空)</p>
        return <div className="text-sm leading-relaxed whitespace-pre-wrap">{parts.map((c, i) => <span key={i}>{c.content}</span>)}</div>
    }

    // 工具调用 (可点击查看详情)
    const renderToolCalls = () => {
        if (!message.tool_calls?.length || !config.showToolLabels) return null
        const sMap: Record<string, string> = { pending: '⏳', in_progress: '🔄', completed: '✅', error: '❌', outdated: '⏰' }
        const cMap: Record<string, string> = { pending: 'bg-yellow-50 text-yellow-700 cursor-pointer', in_progress: 'bg-blue-50 text-blue-700 cursor-pointer', completed: 'bg-green-50 text-green-700 cursor-pointer', error: 'bg-red-50 text-red-700 cursor-pointer', outdated: 'bg-gray-50 text-gray-500 cursor-pointer' }
        return (
            <div className="space-y-1 mt-2">
                {message.tool_calls.map((tc) => (
                    <div key={tc.tool_call_id}
                        onClick={() => setDetailTool(tc)}
                        className={`flex items-center gap-1.5 px-2 py-1 rounded-lg text-xs hover:ring-1 hover:ring-indigo-300 transition ${cMap[tc.status] || 'bg-gray-50'}`}>
                        <span>{sMap[tc.status] || '⚙️'}</span>
                        <span className="font-medium">{tc.tool_name}</span>
                    </div>
                ))}
            </div>
        )
    }

    // TODO
    const renderTodos = () => {
        if (!message.todos?.length) return null
        return (
            <div className="mt-2 space-y-1 border-t border-gray-100 pt-2">
                <div className="text-xs text-gray-400 mb-1">📋 执行计划</div>
                {message.todos.map((t) => (
                    <div key={t.id} className="flex items-center gap-1.5 text-sm">
                        <span>{t.status === 'completed' ? '✅' : '⬜'}</span>
                        <span className={t.status === 'completed' ? 'text-gray-400 line-through' : 'text-gray-700'}>{t.content}</span>
                    </div>
                ))}
            </div>
        )
    }

    // 问题
    const renderQuestions = () => {
        if (!message.questions?.length) return null
        return (
            <div className="mt-3 space-y-2 border-t border-gray-100 pt-2">
                <div className="text-xs text-gray-400 mb-1">💬 请回答</div>
                {message.questions.map((q) => (
                    <div key={q.id} className="bg-gray-50 rounded-lg p-3">
                        <p className="text-sm text-gray-700 mb-2">{q.question}</p>
                        {q.options?.length ? (
                            <div className="flex flex-wrap gap-2">
                                {q.options.map((o) => (
                                    <button key={o.value} className="px-3 py-1 bg-white border border-gray-200 rounded-full text-xs text-gray-600 hover:bg-indigo-50 hover:border-indigo-200 transition">{o.label}</button>
                                ))}
                            </div>
                        ) : q.type === 'input' ? (
                            <input type="text" placeholder="输入回答..." className="w-full px-3 py-1.5 border border-gray-200 rounded-lg text-sm outline-none focus:ring-1 focus:ring-indigo-500" />
                        ) : null}
                    </div>
                ))}
            </div>
        )
    }

    // 图片 (带悬停预览)
    const renderImages = () => {
        if (!message.images?.length) return null
        return (
            <div className="mt-2 grid grid-cols-2 gap-2">
                {message.images.map((img) => (
                    <div key={img.fileId} className="relative group">
                        {img.status === 'loading' ? (
                            <div className="w-full aspect-video bg-gray-100 rounded-lg flex items-center justify-center text-gray-400 text-xs">加载中...</div>
                        ) : img.status === 'error' ? (
                            <div className="w-full aspect-video bg-red-50 rounded-lg flex items-center justify-center text-red-400 text-xs">加载失败</div>
                        ) : (
                            <>
                                <img src={img.base64 || img.url} alt={img.alt || ''}
                                    className="w-full rounded-lg border border-gray-200 cursor-pointer hover:opacity-90 transition"
                                    onClick={() => img.url && window.open(img.url, '_blank')}
                                    onMouseEnter={() => setPreviewImg(img.base64 || img.url || null)}
                                    onMouseLeave={() => setPreviewImg(null)}
                                />
                                {/* 悬停大图预览 */}
                                {previewImg === (img.base64 || img.url) && (
                                    <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 z-50 hidden group-hover:block">
                                        <img src={previewImg} className="max-w-[300px] max-h-[300px] rounded-xl shadow-2xl border border-gray-200" />
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                ))}
            </div>
        )
    }

    // 信息标签
    const renderInfoTags = () => {
        if (!message.info || isHuman) return null
        const tags: { label: string; onClick?: () => void }[] = []
        if (message.info.provider) tags.push({ label: `🔌 ${message.info.provider}` })
        if (message.info.model) tags.push({ label: `🧠 ${message.info.model}` })
        if (message.info.tokens) tags.push({ label: `📊 ${message.info.tokens}` })
        if (message.info.duration) tags.push({ label: `⏱ ${message.info.duration}` })
        if (message.extra?.link_provider) tags.push({ label: '🌐 已访问互联网', onClick: () => alert(JSON.stringify(message.extra, null, 2)) })
        if (!tags.length) return null
        return <div className="flex flex-wrap gap-1 mt-2 pt-2 border-t border-gray-100">{tags.map((t, i) =>
            t.onClick
                ? <button key={i} onClick={t.onClick} className="px-1.5 py-0.5 bg-blue-50 text-blue-600 text-[10px] rounded hover:bg-blue-100 transition cursor-pointer">{t.label}</button>
                : <span key={i} className="px-1.5 py-0.5 bg-gray-100 text-gray-500 text-[10px] rounded">{t.label}</span>
        )}</div>
    }

    // 引用&文件
    const renderReferenced = () => {
        if (!message.referencedMessage) return null
        return <div className="mb-2 px-3 py-2 bg-gray-100 rounded-lg border-l-2 border-gray-300 text-xs text-gray-500 truncate">{message.referencedMessage.content}</div>
    }
    const renderActivedFile = () => {
        if (!message.activedFile) return null
        return <div className="mb-2 px-2 py-1 bg-blue-50 rounded text-xs text-blue-600 flex items-center gap-1">📄 {message.activedFile.name}</div>
    }
    const renderUploadedFiles = () => {
        if (!message.uploadedFiles?.length) return null
        return <div className="mb-2 flex flex-wrap gap-1">{message.uploadedFiles.map((f) => <span key={f.id} className="px-2 py-0.5 bg-gray-100 rounded text-xs text-gray-600">{f.name}</span>)}</div>
    }

    const handleSelectToggle = () => updateMessage(currentHistoryId, message.id, (m) => { m.selected = !m.selected })

    // 右键菜单
    const handleContextMenu = (e: React.MouseEvent) => {
        e.preventDefault()
        const text = message.chunks?.map((c) => c.content).join('') || ''
        setContextMenu({ x: e.clientX, y: e.clientY })
        // 保存当前选中文本供引用
        const sel = window.getSelection()?.toString()
            ; (window as any).__selectedText = sel || text
    }

    const copyText = () => navigator.clipboard.writeText(message.chunks?.map((c) => c.content).join('') || '')

    // 图片预览
    const renderImagePreview = () => {
        if (!previewImg) return null
        return (
            <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-[60] cursor-zoom-out" onClick={() => setPreviewImg(null)}>
                <img src={previewImg} className="max-w-[90vw] max-h-[90vh] rounded-xl shadow-2xl" />
            </div>
        )
    }

    return (
        <div className={`flex ${isHuman ? 'justify-end' : 'justify-start'} group`} onMouseEnter={() => setShowActions(true)} onMouseLeave={() => setShowActions(false)} onContextMenu={handleContextMenu}>
            <div className={`max-w-[85%] rounded-2xl px-4 py-3 relative transition ${isHuman
                ? message.selected ? 'bg-indigo-400 text-white rounded-br-md ring-2 ring-indigo-300' : 'bg-indigo-500 text-white rounded-br-md'
                : message.selected ? 'bg-indigo-50 border border-indigo-200 rounded-bl-md ring-2 ring-indigo-200' : 'bg-white border border-gray-200 shadow-sm rounded-bl-md'
                }`}>
                {/* 选择框 */}
                {selectMode && (
                    <div className="absolute -left-7 top-1/2 -translate-y-1/2">
                        <input type="checkbox" checked={!!message.selected} onChange={handleSelectToggle} className="rounded cursor-pointer" />
                    </div>
                )}

                {renderReferenced()}
                {renderActivedFile()}
                {renderUploadedFiles()}

                {message.label && !isHuman && (
                    <div className="text-xs text-gray-400 mb-1 flex items-center gap-2">
                        <span>{message.label}</span>
                        {message.pending && <span className="inline-block w-2 h-2 bg-yellow-400 rounded-full animate-pulse" />}
                    </div>
                )}

                {editing ? (
                    <div className="space-y-2">
                        <textarea value={editText} onChange={(e) => setEditText(e.target.value)} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500 resize-none text-gray-900" rows={4} autoFocus />
                        <div className="flex gap-2 justify-end">
                            <button onClick={() => setEditing(false)} className="px-3 py-1 text-xs text-gray-500 hover:bg-gray-100 rounded">取消</button>
                            <button onClick={submitEdit} className="px-3 py-1 text-xs bg-indigo-500 text-white rounded hover:bg-indigo-600">重新发送</button>
                        </div>
                    </div>
                ) : (
                    <>
                        {renderThink()}
                        {renderContent()}
                        {renderToolCalls()}
                        {renderTodos()}
                        {renderQuestions()}
                        {renderImages()}
                        {renderInfoTags()}
                        {message.pending && (!message.chunks || message.chunks.length === 0) && (
                            <div className="flex gap-1 py-2">
                                <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                                <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                                <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                            </div>
                        )}
                    </>
                )}

                {/* 操作栏 */}
                {showActions && !editing && !selectMode && (
                    <div className={`absolute ${isHuman ? '-left-1' : '-right-1'} -top-3 flex gap-1`}>
                        {isHuman && onEditResend && <button onClick={startEdit} className="px-2 py-1 bg-white border border-gray-200 rounded-lg shadow-sm text-xs hover:text-indigo-600 transition" title="编辑">✏️</button>}
                        <button onClick={copyText} className="px-2 py-1 bg-white border border-gray-200 rounded-lg shadow-sm text-xs hover:text-gray-700 transition" title="复制">📋</button>
                        {!isHuman && onRegenerate && <button onClick={() => onRegenerate(message.id)} className="px-2 py-1 bg-white border border-gray-200 rounded-lg shadow-sm text-xs hover:text-indigo-600 transition" title="重新生成">🔄</button>}
                        {message.pre_node && <button onClick={() => onBranchNavigate?.(message.id, 'pre')} className="px-2 py-1 bg-white border border-gray-200 rounded-lg shadow-sm text-xs hover:text-gray-700 transition" title="上一分支">◀</button>}
                        {message.next_node && <button onClick={() => onBranchNavigate?.(message.id, 'next')} className="px-2 py-1 bg-white border border-gray-200 rounded-lg shadow-sm text-xs hover:text-gray-700 transition" title="下一分支">▶</button>}
                    </div>
                )}

                {/* 右键菜单 */}
                {contextMenu && (
                    <ContextMenu
                        x={contextMenu.x}
                        y={contextMenu.y}
                        onClose={() => setContextMenu(null)}
                        items={[
                            { label: '复制', icon: '📋', onClick: copyText },
                            ...(isHuman && onEditResend ? [{ label: '编辑', icon: '✏️', onClick: startEdit }] : []),
                            ...(!isHuman && onRegenerate ? [{ label: '重新生成', icon: '🔄', onClick: () => onRegenerate(message.id) }] : []),
                            { label: '引用', icon: '💬', onClick: () => { const t = (window as any).__selectedText || ''; onQuote?.(t) } },
                            { label: '删除', icon: '🗑', onClick: () => deleteMessages(currentHistoryId, [message.id]), danger: true, divider: true },
                        ]}
                    />
                )}

                {/* 工具详情弹窗 */}
                {detailTool && <ToolDetailDialog toolCall={detailTool} onClose={() => setDetailTool(null)} />}

                {renderImagePreview()}
            </div>
        </div>
    )
}
