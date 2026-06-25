/* ======== 消息类型 ======== */

export interface MessageChunk {
    content: string
    label_type: 'think' | 'content'
}

export interface ToolCallInfo {
    tool_call_id: string
    tool_name: string
    content: unknown
    status: 'pending' | 'in_progress' | 'completed' | 'error' | 'outdated'
}

export interface TodoItem {
    id: string
    content: string
    status: 'pending' | 'completed'
}

export interface QuestionOption {
    label: string
    value: string
}

export interface QuestionItem {
    id: string
    type: 'single' | 'multi' | 'input'
    question: string
    options?: QuestionOption[]
}

export interface ImageItem {
    fileId: string
    base64?: string
    url?: string
    alt?: string
    status?: 'loading' | 'loaded' | 'error'
}

export interface UploadedFile {
    id: string
    name: string
    size: number
    type: string
    data?: File
    progress?: number
}

export interface ChatMessage {
    id: string
    cid: string
    hid: string
    role: 'human' | 'ai' | 'system' | 'tools' | 'info'
    node_id?: string
    parent_id?: string
    pre_node?: string
    next_node?: string
    chunks?: MessageChunk[]
    tool_calls?: ToolCallInfo[]
    todos?: TodoItem[]
    questions?: QuestionItem[]
    images?: ImageItem[]
    label?: string
    info?: Record<string, unknown>
    extra?: Record<string, unknown>
    uploadedFiles?: UploadedFile[]
    referencedMessage?: { id: string; content: string }
    activedFile?: { path: string; name: string }
    pending?: boolean
    error?: boolean
    selected?: boolean
    editing?: boolean
    collapsed?: boolean
}

/* ======== Agent 配置 ======== */

export interface RolePrompt {
    name: string
    definition: string
}

export interface AgentConfig {
    modelsProvider: string
    modelName: string
    apiKey: string
    modelTemperature: number
    enableThink: boolean
    pureChatOn: boolean
    workDir: string
    enableFileOperation: boolean
    enableWebSearch: boolean
    enableKnowledgeRetrieval: boolean
    enableCommandOperation: boolean
    enableAgentAssign: boolean
    enableShorttermMemory: boolean
    enableLongtermMemory: boolean
    rolePrompt: RolePrompt
    showToolLabels: boolean
    darkTheme: boolean
    httpProxyUrl: string
    httpsProxyUrl: string
    excludeUrl: string
    linkProvider: string
    linkApiKey: string
    contentProvider: string
    contentApiKey: string
    webContentFilter: string
    excludeWebUrl: string
    skillLoad: boolean
    visionOn: boolean
    agentSwarm: boolean
    messageSummary: number
    keepNotSummary: number
    embeddingModel: string
    alwaysQuoteFile: boolean
    autoRefreshTask: boolean
    enableTaskFlow: boolean
    // 补齐剩余配置
    tokenLimit: number
    remainToolsCache: boolean
    higherRolePromptPermission: boolean
    autoSaveConfig: boolean
    backgroundImage: string
}

export const DEFAULT_CONFIG: AgentConfig = {
    modelsProvider: '',
    modelName: '',
    apiKey: '',
    modelTemperature: 0.7,
    enableThink: false,
    pureChatOn: false,
    workDir: '',
    enableFileOperation: false,
    enableWebSearch: false,
    enableKnowledgeRetrieval: false,
    enableCommandOperation: false,
    enableAgentAssign: false,
    enableShorttermMemory: true,
    enableLongtermMemory: true,
    rolePrompt: { name: '', definition: '' },
    showToolLabels: true,
    darkTheme: false,
    httpProxyUrl: '',
    httpsProxyUrl: '',
    excludeUrl: '',
    linkProvider: '',
    linkApiKey: '',
    contentProvider: '',
    contentApiKey: '',
    webContentFilter: 'llm',
    excludeWebUrl: '',
    skillLoad: false,
    visionOn: true,
    agentSwarm: false,
    messageSummary: 128,
    keepNotSummary: 64,
    embeddingModel: '',
    alwaysQuoteFile: false,
    autoRefreshTask: false,
    enableTaskFlow: false,
    tokenLimit: 0,
    remainToolsCache: false,
    higherRolePromptPermission: false,
    autoSaveConfig: false,
    backgroundImage: '',
}

/* ======== 供应商 (Provider) ======== */

export interface Provider {
    provider_id: string
    name: string
    endpoint: string
    api_key: string
    type: string
    description: string
    model_list: string[]
    enabled: boolean
    updated_at: string
}

/* ======== 知识库 (RAG) ======== */

export interface RagDocument {
    doc_id: string
    name: string
    description: string
    file_type: string
    file_size: number
    status: 'indexing' | 'indexed' | 'failed'
    enabled: boolean
    created_at: string
}

/* ======== 技能包 (Skill) ======== */

export interface Skill {
    skill_id: string
    name: string
    description: string
    version: string
    enabled: boolean
    created_at: string
}

/* ======== 角色卡 (Role) ======== */

export interface Role {
    role_id: string
    name: string
    definition: string
    enabled: boolean
    created_at: string
}

/* ======== MCP 服务 ======== */

export interface McpService {
    mcp_id: string
    name: string
    endpoint: string
    type: 'stdio' | 'streamable_http'
    description: string
    enabled: boolean
    created_at: string
}

/* ======== 后台任务 ======== */

export interface AgentTask {
    task_id: string
    goal: string
    agent_name: string
    status: 'pending' | 'running' | 'completed' | 'failed'
    progress: string
    created_at: string
}

export interface TaskStats {
    total: number
    pending: number
    running: number
    completed: number
    failed: number
}

/* ======== 文件系统 ======== */

export interface FileNode {
    name: string
    path: string
    type: 'file' | 'directory'
    children?: FileNode[]
    size?: number
    modified?: string
}

/* ======== 会话 ======== */

export interface Conversation {
    id: string
    title: string
    starred: boolean
    updatedAt: string
    messageCount: number
    workDir?: string
}

/* ======== WebSocket 事件 ======== */

export interface WsPayload {
    generation_id: string
    data: {
        messages: {
            event_name: string
            content?: string
            [key: string]: unknown
        }
        history_id: string
    }
}
