import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { ChatMessage, AgentConfig, Conversation } from '../lib/types'
import { DEFAULT_CONFIG } from '../lib/types'

interface ChatState {
    // 当前会话
    currentHistoryId: string
    setCurrentHistoryId: (id: string) => void

    // 消息缓存: historyId -> ChatMessage[]
    messages: Record<string, ChatMessage[]>
    getMessages: (hid: string) => ChatMessage[]
    appendMessage: (hid: string, msg: ChatMessage) => void
    updateMessage: (hid: string, msgId: string, updater: (msg: ChatMessage) => void) => void
    updateLastAiMessage: (hid: string, updater: (msg: ChatMessage) => void) => void
    deleteMessages: (hid: string, msgIds: string[]) => void

    // 生成状态
    generating: Record<string, boolean>
    setGenerating: (hid: string, v: boolean) => void

    // 会话列表
    conversations: Conversation[]
    addConversation: (conv: Conversation) => void
    updateConversation: (id: string, updater: (conv: Conversation) => void) => void
    deleteConversation: (id: string) => void

    // 会话工作目录映射
    workDirMap: Record<string, string>
    setWorkDirForHistory: (hid: string, dir: string) => void

    // Agent 配置
    config: AgentConfig
    setConfig: (cfg: Partial<AgentConfig>) => void

    // 选择模式
    selectMode: boolean
    setSelectMode: (v: boolean) => void

    // WebSocket
    ws: WebSocket | null
    setWs: (ws: WebSocket | null) => void

    // 警告横幅
    warningBanner: boolean
    dismissWarning: () => void
}

export const useChatStore = create<ChatState>()(
    persist(
        (set, get) => ({
            currentHistoryId: '',
            setCurrentHistoryId: (id) => set({ currentHistoryId: id }),

            messages: {},
            getMessages: (hid) => get().messages[hid] || [],
            appendMessage: (hid, msg) =>
                set((s) => ({
                    messages: {
                        ...s.messages,
                        [hid]: [...(s.messages[hid] || []), msg],
                    },
                })),
            updateMessage: (hid, msgId, updater) =>
                set((s) => {
                    const list = s.messages[hid]
                    if (!list) return s
                    const idx = list.findIndex((m) => m.id === msgId)
                    if (idx === -1) return s
                    const updated = [...list]
                    updater(updated[idx])
                    return { messages: { ...s.messages, [hid]: updated } }
                }),
            updateLastAiMessage: (hid, updater) =>
                set((s) => {
                    const list = s.messages[hid]
                    if (!list) return s
                    const idx = list.length - 1
                    if (idx < 0 || list[idx].role !== 'ai') return s
                    const updated = [...list]
                    updater(updated[idx])
                    return { messages: { ...s.messages, [hid]: updated } }
                }),
            deleteMessages: (hid, msgIds) =>
                set((s) => ({
                    messages: {
                        ...s.messages,
                        [hid]: (s.messages[hid] || []).filter((m) => !msgIds.includes(m.id)),
                    },
                })),

            generating: {},
            setGenerating: (hid, v) => set((s) => ({ generating: { ...s.generating, [hid]: v } })),

            conversations: [],
            addConversation: (conv) =>
                set((s) => ({ conversations: [conv, ...s.conversations] })),
            updateConversation: (id, updater) =>
                set((s) => ({
                    conversations: s.conversations.map((c) =>
                        c.id === id ? (updater(c), c) : c
                    ),
                })),
            deleteConversation: (id) =>
                set((s) => ({
                    conversations: s.conversations.filter((c) => c.id !== id),
                })),

            workDirMap: {},
            setWorkDirForHistory: (hid, dir) =>
                set((s) => ({ workDirMap: { ...s.workDirMap, [hid]: dir } })),

            config: { ...DEFAULT_CONFIG },
            setConfig: (cfg) => set((s) => ({ config: { ...s.config, ...cfg } })),

            selectMode: false,
            setSelectMode: (v) => set({ selectMode: v }),

            ws: null,
            setWs: (ws) => set({ ws }),

            warningBanner: true,
            dismissWarning: () => set({ warningBanner: false }),
        }),
        {
            name: 'apix-chat-store',
            partialize: (state) => ({
                config: state.config,
                conversations: state.conversations,
                workDirMap: state.workDirMap,
                messages: state.messages,
                warningBanner: state.warningBanner,
            }),
        }
    )
)
