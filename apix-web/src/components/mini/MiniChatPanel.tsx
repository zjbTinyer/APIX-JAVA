import { useState, useRef, useCallback, useEffect } from 'react'
import { useChatStore } from '../../store/chatStore'
import { useAuthStore } from '../../store/authStore'
import { buildWsUrl } from '../../lib/api'
import type { ChatMessage, WsPayload } from '../../lib/types'

interface Props {
    pageId: string
    activedFilePath?: string
    activedFileName?: string
    onClose?: () => void
}

export default function MiniChatPanel({ pageId, activedFilePath, activedFileName, onClose }: Props) {
    const { config } = useChatStore()
    const { user } = useAuthStore()
    const [width, setWidth] = useState(360)
    const [text, setText] = useState('')
    const [messages, setMessages] = useState<ChatMessage[]>([])
    const [ws, setWs] = useState<WebSocket | null>(null)
    const [connected, setConnected] = useState(false)
    const [alwaysQuote, setAlwaysQuote] = useState(false)
    const bottomRef = useRef<HTMLDivElement>(null)
    const textareaRef = useRef<HTMLTextAreaElement>(null)
    const resizeRef = useRef<{ startX: number; startW: number } | null>(null)
    const [historyId, setHistoryId] = useState<string>('')
    const [showHistory, setShowHistory] = useState(true)

    // WebSocket
    useEffect(() => {
        if (!user) return
        const socket = new WebSocket(buildWsUrl(user.userUid))
        socket.onopen = () => { setConnected(true); setWs(socket) }
        socket.onclose = () => { setConnected(false); setWs(null) }
        socket.onmessage = (event) => {
            try {
                const payload: WsPayload = JSON.parse(event.data)
                const eventName = payload.data?.messages?.event_name
                const hid = payload.data?.history_id || historyId

                switch (eventName) {
                    case 'content_chunk_rtn': {
                        const c = payload.data.messages.content || ''
                        setMessages((prev) => {
                            const list = [...prev]
                            const last = list[list.length - 1]
                            if (last?.role === 'ai') {
                                if (!last.chunks) last.chunks = []
                                const lastChunk = last.chunks[last.chunks.length - 1]
                                if (lastChunk?.label_type === 'content') lastChunk.content += c
                                else last.chunks.push({ content: c, label_type: 'content' })
                            }
                            return list
                        })
                        break
                    }
                    case 'think_chunk_rtn': {
                        const c = payload.data.messages.content || ''
                        setMessages((prev) => {
                            const list = [...prev]
                            const last = list[list.length - 1]
                            if (last?.role === 'ai') {
                                if (!last.chunks) last.chunks = []
                                const lastChunk = last.chunks[last.chunks.length - 1]
                                if (lastChunk?.label_type === 'think') lastChunk.content += c
                                else last.chunks.push({ content: c, label_type: 'think' })
                            }
                            return list
                        })
                        break
                    }
                    case 'msg_stream_start': {
                        setMessages((prev) => [...prev, {
                            id: payload.generation_id, cid: user?.userUid || '', hid, role: 'ai',
                            chunks: [], pending: true, label: '正在思考',
                        }])
                        break
                    }
                    case 'msg_stream_end':
                        setMessages((prev) => prev.map((m) => m.pending ? { ...m, pending: false, label: '已思考' } : m))
                        break
                }
                bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
            } catch { }
        }
        return () => { socket.close() }
    }, [user?.userUid])

    // 发送
    const handleSend = useCallback(() => {
        const trimmed = text.trim()
        if (!trimmed || !ws || ws.readyState !== WebSocket.OPEN) return
        const hid = historyId || `mini_${pageId}_${Date.now()}`
        if (!historyId) setHistoryId(hid)

        setMessages((prev) => [...prev, {
            id: `msg_${Date.now()}`, cid: user?.userUid || '', hid, role: 'human',
            chunks: [{ content: trimmed, label_type: 'content' }],
            activedFile: alwaysQuote && activedFilePath ? { path: activedFilePath, name: activedFileName || activedFilePath } : undefined,
        }])

        const msgContent = alwaysQuote && activedFilePath
            ? `[文件: ${activedFilePath}]\n${trimmed}`
            : trimmed

        ws.send(JSON.stringify({
            action: 'chat_with_llm',
            data: {
                client_id: user?.userUid, history_id: hid, platform: 'web',
                messages: { content: msgContent },
                re_generate: false,
                config: {
                    modelsProvider: config.modelsProvider, modelName: config.modelName, apiKey: config.apiKey,
                    enableThink: config.enableThink, pureChatOn: config.pureChatOn, workDir: config.workDir,
                    enableFileOperation: config.enableFileOperation, enableWebSearch: config.enableWebSearch,
                    enableKnowledgeRetrieval: config.enableKnowledgeRetrieval, enableCommandOperation: config.enableCommandOperation,
                    enableAgentAssign: config.enableAgentAssign, enableShorttermMemory: config.enableShorttermMemory,
                    enableLongtermMemory: config.enableLongtermMemory,
                },
            },
        }))
        setText('')
    }, [text, ws, historyId, user, config, pageId, alwaysQuote, activedFilePath, activedFileName])

    // 调整宽度
    const startResize = useCallback((e: React.MouseEvent) => {
        e.preventDefault()
        resizeRef.current = { startX: e.clientX, startW: width }
        const onMove = (ev: MouseEvent) => {
            if (resizeRef.current) setWidth(Math.max(280, Math.min(600, resizeRef.current.startW - (ev.clientX - resizeRef.current.startX))))
        }
        const onUp = () => { resizeRef.current = null; document.removeEventListener('mousemove', onMove); document.removeEventListener('mouseup', onUp) }
        document.addEventListener('mousemove', onMove)
        document.addEventListener('mouseup', onUp)
    }, [width])

    return (
        <div className="h-full flex flex-col bg-white border-l border-gray-200 relative shrink-0" style={{ width }}>
            {/* 拖拽手柄 */}
            <div className="absolute left-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-indigo-400 z-10" onMouseDown={startResize} />

            {/* 头部 */}
            <div className="flex items-center justify-between px-3 py-2 border-b border-gray-100 shrink-0">
                <div className="flex items-center gap-2">
                    {historyId ? (
                        <button onClick={() => { setMessages([]); setHistoryId('') }} className="text-gray-400 hover:text-gray-600 text-sm" title="返回">←</button>
                    ) : (
                        <span className="text-gray-300 text-sm">+</span>
                    )}
                    <span className="text-sm font-medium text-gray-700">{historyId ? '会话' : '新聊天'}</span>
                </div>
                <div className="flex items-center gap-1">
                    <button
                        onClick={() => setAlwaysQuote(!alwaysQuote)}
                        className={`text-xs px-2 py-0.5 rounded transition ${alwaysQuote ? 'bg-indigo-100 text-indigo-700' : 'text-gray-400 hover:text-gray-600'}`}
                    >
                        引用此文件
                    </button>
                    {onClose && (
                        <button onClick={onClose} className="text-gray-300 hover:text-gray-600 text-xs ml-1">✕</button>
                    )}
                </div>
            </div>

            {/* 消息列表 */}
            <div className="flex-1 overflow-y-auto p-3 space-y-3">
                {messages.length === 0 ? (
                    <div className="text-center text-gray-400 text-xs py-10">
                        <p className="mb-1">Mini Chat</p>
                        <p>输入消息开始对话</p>
                        {activedFileName && <p className="text-indigo-400 mt-2">当前文件: {activedFileName}</p>}
                    </div>
                ) : (
                    messages.map((msg) => {
                        const isHuman = msg.role === 'human'
                        return (
                            <div key={msg.id} className={`flex ${isHuman ? 'justify-end' : 'justify-start'}`}>
                                <div className={`max-w-[85%] rounded-xl px-3 py-2 text-sm ${isHuman ? 'bg-indigo-500 text-white' : 'bg-gray-100 text-gray-700'
                                    }`}>
                                    {msg.activedFile && (
                                        <div className="text-xs mb-1 opacity-70">📄 {msg.activedFile.name}</div>
                                    )}
                                    {msg.chunks?.map((c, i) => (
                                        c.label_type === 'think'
                                            ? <details key={i} className="text-xs text-gray-400"><summary>思考</summary>{c.content}</details>
                                            : <span key={i}>{c.content}</span>
                                    ))}
                                    {msg.pending && <span className="animate-pulse">...</span>}
                                </div>
                            </div>
                        )
                    })
                )}
                <div ref={bottomRef} />
            </div>

            {/* 活动文件提示 */}
            {alwaysQuote && activedFileName && (
                <div className="px-3 py-1 bg-indigo-50 border-t border-indigo-100 text-xs text-indigo-600 flex items-center gap-1 shrink-0">
                    📄 始终引用: {activedFileName}
                </div>
            )}

            {/* 输入区 */}
            <div className="border-t border-gray-100 p-3 shrink-0">
                <div className="flex gap-2">
                    <textarea
                        ref={textareaRef}
                        value={text}
                        onChange={(e) => setText(e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend() } }}
                        placeholder="输入消息..."
                        className="flex-1 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-xs outline-none resize-none focus:ring-1 focus:ring-indigo-500"
                        rows={2}
                    />
                </div>
                <div className="flex justify-between mt-1">
                    <span className="text-[10px] text-gray-400">{connected ? '🟢' : '🔴'}</span>
                    <button
                        onClick={handleSend}
                        disabled={!text.trim() || !connected}
                        className="px-3 py-1 bg-indigo-500 hover:bg-indigo-600 disabled:bg-gray-200 text-white text-xs rounded-lg transition"
                    >
                        发送
                    </button>
                </div>
            </div>
        </div>
    )
}
