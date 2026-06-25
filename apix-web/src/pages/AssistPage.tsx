import { useEffect, useRef, useState, useCallback } from 'react'
import { useChatStore } from '../store/chatStore'
import { useAuthStore } from '../store/authStore'
import { buildWsUrl } from '../lib/api'
import type { ChatMessage, WsPayload, UploadedFile, ToolCallInfo } from '../lib/types'
import MessageBubble from '../components/MessageBubble'
import ChatInput from '../components/ChatInput'
import HistoryPanel from '../components/HistoryPanel'
import MessageScrollBar from '../components/MessageScrollBar'
import { WithSelection } from '../components/SelectionBubble'

const WELCOME_TEXT = '你好！我是 APIX AI 助手，有什么可以帮助你的？'

export default function AssistPage() {
    const {
        currentHistoryId, setCurrentHistoryId,
        messages, getMessages, appendMessage, updateLastAiMessage,
        generating, setGenerating,
        config, selectMode, setSelectMode, deleteMessages,
        warningBanner, dismissWarning,
        workDirMap, setWorkDirForHistory,
    } = useChatStore()
    const { user } = useAuthStore()
    const bottomRef = useRef<HTMLDivElement>(null)
    const [connected, setConnected] = useState(false)
    const wsRef = useRef<WebSocket | null>(null)
    const [typewriter, setTypewriter] = useState('')
    const [typewriterDone, setTypewriterDone] = useState(false)
    const msgListRef = useRef<HTMLDivElement>(null)

    const scrollToBottom = useCallback(() => {
        setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 50)
    }, [])

    const jumpToMessage = useCallback((index: number) => {
        const el = msgListRef.current?.children[index] as HTMLElement
        el?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }, [])

    // 打字机效果
    useEffect(() => {
        if (getMessages(currentHistoryId).length > 0) { setTypewriterDone(true); return }
        let i = 0
        setTypewriter('')
        setTypewriterDone(false)
        const timer = setInterval(() => {
            i++
            setTypewriter(WELCOME_TEXT.slice(0, i))
            if (i >= WELCOME_TEXT.length) { clearInterval(timer); setTypewriterDone(true) }
        }, 40)
        return () => clearInterval(timer)
    }, [currentHistoryId])

    // WebSocket
    useEffect(() => {
        if (!user) return
        const url = buildWsUrl(user.userUid)
        const socket = new WebSocket(url)
        socket.onopen = () => { setConnected(true); wsRef.current = socket }
        socket.onclose = () => { setConnected(false); wsRef.current = null }
        socket.onmessage = (event) => {
            try {
                const payload: WsPayload = JSON.parse(event.data)
                const eventName = payload.data?.messages?.event_name
                const hid = payload.data?.history_id || currentHistoryId
                switch (eventName) {
                    case 'content_chunk_rtn': {
                        const c = payload.data.messages.content || ''
                        updateLastAiMessage(hid, (msg) => {
                            if (!msg.chunks) msg.chunks = []
                            const last = msg.chunks[msg.chunks.length - 1]
                            if (last?.label_type === 'content') last.content += c
                            else msg.chunks.push({ content: c, label_type: 'content' })
                        })
                        break
                    }
                    case 'think_chunk_rtn': {
                        const c = payload.data.messages.content || ''
                        updateLastAiMessage(hid, (msg) => {
                            if (!msg.chunks) msg.chunks = []
                            const last = msg.chunks[msg.chunks.length - 1]
                            if (last?.label_type === 'think') last.content += c
                            else msg.chunks.push({ content: c, label_type: 'think' })
                        })
                        break
                    }
                    case 'tool_call_rtn': {
                        const tc = payload.data.messages as Record<string, unknown>
                        updateLastAiMessage(hid, (msg) => {
                            if (!msg.tool_calls) msg.tool_calls = []
                            msg.tool_calls.push({
                                tool_call_id: String(tc.tool_call_id || ''),
                                tool_name: String(tc.tool_name || ''),
                                content: tc.content,
                                status: (tc.status as ToolCallInfo['status']) || 'pending',
                            })
                        })
                        break
                    }
                    case 'msg_stream_start': {
                        const genId = payload.generation_id
                        appendMessage(hid, {
                            id: genId, cid: user?.userUid || '', hid, role: 'ai',
                            chunks: [], pending: true, label: '正在思考',
                        })
                        setGenerating(hid, true)
                        break
                    }
                    case 'msg_stream_end':
                        setGenerating(hid, false)
                        updateLastAiMessage(hid, (m) => { m.pending = false; m.label = '已思考' })
                        break
                    case 'msg_stream_abort':
                        setGenerating(hid, false)
                        updateLastAiMessage(hid, (m) => { m.pending = false; m.label = '思考中断' })
                        break
                    case 'msg_stream_error':
                        setGenerating(hid, false)
                        updateLastAiMessage(hid, (m) => { m.pending = false; m.label = '出错了'; m.error = true })
                        break
                }
                scrollToBottom()
            } catch (e) { console.error('[WS] err', e) }
        }
        return () => { socket.close() }
    }, [user?.userUid])

    // 发送消息
    const sendMessage = useCallback((content: string, files?: UploadedFile[]) => {
        if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return
        const hid = currentHistoryId || `conv_${Date.now()}`
        if (!currentHistoryId) setCurrentHistoryId(hid)

        // 保存会话工作目录
        if (config.workDir) setWorkDirForHistory(hid, config.workDir)

        appendMessage(hid, {
            id: `msg_${Date.now()}`,
            cid: user?.userUid || '', hid, role: 'human',
            chunks: [{ content: content || '(文件)', label_type: 'content' }],
            uploadedFiles: files,
        })

        wsRef.current.send(JSON.stringify({
            action: 'chat_with_llm',
            data: {
                client_id: user?.userUid, history_id: hid, platform: 'web',
                messages: { content },
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
        scrollToBottom()
    }, [currentHistoryId, user, config, appendMessage, setCurrentHistoryId, scrollToBottom])

    // 重新生成
    const handleRegenerate = useCallback((msgId: string) => {
        const list = getMessages(currentHistoryId)
        for (let i = list.findIndex((m) => m.id === msgId) - 1; i >= 0; i--) {
            if (list[i].role === 'human') {
                sendMessage(list[i].chunks?.map((c) => c.content).join('') || '')
                return
            }
        }
    }, [currentHistoryId, getMessages, sendMessage])

    // 批量删除
    const handleBatchDelete = () => {
        const selected = getMessages(currentHistoryId).filter((m) => m.selected).map((m) => m.id)
        if (selected.length) deleteMessages(currentHistoryId, selected)
        setSelectMode(false)
    }

    const msgList = getMessages(currentHistoryId)

    return (
        <div className="h-full flex">
            <HistoryPanel />

            <div className="flex-1 flex flex-col">
                {/* 顶部栏 */}
                <div className="h-9 bg-gray-50 border-b border-gray-200 flex items-center justify-between px-4 text-xs shrink-0">
                    <div className="flex items-center gap-3">
                        <span className={`inline-block w-2 h-2 rounded-full ${connected ? 'bg-green-500' : 'bg-red-500'}`} />
                        <span className="text-gray-500">{connected ? '已连接' : '未连接'}</span>
                        {config.workDir ? (
                            <span className="text-gray-400 flex items-center gap-1">📂 <span className="truncate max-w-[200px]">{config.workDir}</span></span>
                        ) : (
                            <span className="text-gray-300">未指定工作目录</span>
                        )}
                    </div>
                    <div className="flex items-center gap-2">
                        <button onClick={() => setSelectMode(!selectMode)} className={`px-2 py-0.5 rounded text-xs transition ${selectMode ? 'bg-indigo-100 text-indigo-700' : 'text-gray-400 hover:text-gray-600'}`}>
                            ☑ 选择
                        </button>
                        {selectMode && (
                            <button onClick={handleBatchDelete} className="px-2 py-0.5 bg-red-100 text-red-600 rounded text-xs hover:bg-red-200">
                                删除选中 ({getMessages(currentHistoryId).filter((m) => m.selected).length})
                            </button>
                        )}
                    </div>
                </div>

                {/* 警告横幅 */}
                {warningBanner && (
                    <div className="flex items-center justify-between px-4 py-2 bg-amber-50 border-b border-amber-200 text-xs text-amber-700 shrink-0">
                        <span>⚠️ 请先在设置页配置 LLM 供应商和 API Key，否则无法正常对话</span>
                        <button onClick={dismissWarning} className="text-amber-400 hover:text-amber-600 ml-2 shrink-0">✕</button>
                    </div>
                )}

                {/* 消息列表 */}
                <div className="flex-1 overflow-y-auto px-4 py-4 relative" ref={msgListRef}>
                    {msgList.length > 0 && (
                        <MessageScrollBar messages={msgList} onJumpTo={jumpToMessage} />
                    )}
                    {msgList.length === 0 ? (
                        <div className="h-full flex flex-col items-center justify-center text-gray-400">
                            <div className="text-6xl mb-4">🤖</div>
                            <p className="text-lg font-medium mb-1">APIX AI 助手</p>
                            <p className="text-sm min-h-[1.5em]">
                                {typewriter}{!typewriterDone && <span className="animate-pulse ml-0.5">|</span>}
                            </p>
                        </div>
                    ) : (
                        <WithSelection onQuote={(text: string) => text && wsRef.current?.readyState === WebSocket.OPEN && sendMessage(text)}>
                            <div className="max-w-3xl mx-auto space-y-4">
                                {msgList.map((msg) => (
                                    <MessageBubble
                                        key={msg.id}
                                        message={msg}
                                        onEditResend={(msgId, newContent) => sendMessage(newContent)}
                                        onRegenerate={handleRegenerate}
                                        onQuote={(text: string) => text && wsRef.current?.readyState === WebSocket.OPEN && sendMessage(text)}
                                    />
                                ))}
                            </div>
                        </WithSelection>
                    )}
                    <div ref={bottomRef} />
                </div>

                {/* 输入区 */}
                <ChatInput
                    onSend={sendMessage}
                    disabled={!connected}
                    generating={generating[currentHistoryId]}
                    onStop={() => {
                        wsRef.current?.send(JSON.stringify({
                            action: 'abort_generation',
                            data: { client_id: user?.userUid, history_id: currentHistoryId, platform: 'web' },
                        }))
                    }}
                />
            </div>
        </div>
    )
}
