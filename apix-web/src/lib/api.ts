import CryptoJS from 'crypto-js'
import type { AgentConfig, Provider, RagDocument, Skill, Role, McpService, AgentTask } from './types'
import { useAuthStore } from '../store/authStore'

const API_BASE = ''

/** AES 加密 — 与 Electron 前端兼容 */
function aesEncrypt(text: string): string {
    const key = CryptoJS.enc.Utf8.parse('0123456789abcdef')
    const iv = CryptoJS.enc.Utf8.parse('abcdef9876543210')
    const encrypted = CryptoJS.AES.encrypt(text, key, {
        iv,
        mode: CryptoJS.mode.CBC,
        padding: CryptoJS.pad.Pkcs7,
    })
    return encrypted.toString()
}

/* ======== Auth ======== */

export async function register(username: string, password: string) {
    const res = await fetch(`${API_BASE}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password: aesEncrypt(password) }),
    })
    return res.json()
}

export async function login(username: string, password: string) {
    const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password: aesEncrypt(password) }),
    })
    return res.json()
}

export async function ensureUser(clientId: string) {
    const res = await fetch(`${API_BASE}/auth/ensure_user`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ client_id: clientId }),
    })
    return res.json()
}

/* ======== Chat ======== */

export async function getModelsList(provider: string, apiKey: string) {
    const res = await fetch(`${API_BASE}/api/v1/get_models_list`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ model_provider: provider, api_key: apiKey }),
    })
    return res.json()
}

export function buildWsUrl(clientId: string): string {
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${location.hostname}:5091/ws/default/${clientId}`
}

/* ======== 通用请求工具 ======== */

async function authPost(path: string, body: unknown) {
    const user = useAuthStore.getState().user
    const token = user?.token
    const res = await fetch(`${API_BASE}${path}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(body),
    })
    return res.json()
}

/* ======== 供应商 (Provider) ======== */

// 数据存储在 localStorage，后续对接后端 CRUD
const PROVIDERS_KEY = 'apix_providers'

export function getProviders(): Provider[] {
    try { return JSON.parse(localStorage.getItem(PROVIDERS_KEY) || '[]') } catch { return [] }
}
export function saveProviders(list: Provider[]) {
    localStorage.setItem(PROVIDERS_KEY, JSON.stringify(list))
}

/* ======== 知识库 (RAG) ======== */

const RAG_KEY = 'apix_rag_docs'

export function getRagDocuments(): RagDocument[] {
    try { return JSON.parse(localStorage.getItem(RAG_KEY) || '[]') } catch { return [] }
}
export function saveRagDocuments(list: RagDocument[]) {
    localStorage.setItem(RAG_KEY, JSON.stringify(list))
}

/* ======== 技能包 (Skill) ======== */

const SKILL_KEY = 'apix_skills'

export function getSkills(): Skill[] {
    try { return JSON.parse(localStorage.getItem(SKILL_KEY) || '[]') } catch { return [] }
}
export function saveSkills(list: Skill[]) {
    localStorage.setItem(SKILL_KEY, JSON.stringify(list))
}

/* ======== 角色卡 (Role) ======== */

const ROLE_KEY = 'apix_roles'

export function getRoles(): Role[] {
    try { return JSON.parse(localStorage.getItem(ROLE_KEY) || '[]') } catch { return [] }
}
export function saveRoles(list: Role[]) {
    localStorage.setItem(ROLE_KEY, JSON.stringify(list))
}

/* ======== MCP 服务 ======== */

const MCP_KEY = 'apix_mcp_services'

export function getMcpServices(): McpService[] {
    try { return JSON.parse(localStorage.getItem(MCP_KEY) || '[]') } catch { return [] }
}
export function saveMcpServices(list: McpService[]) {
    localStorage.setItem(MCP_KEY, JSON.stringify(list))
}

/* ======== 后台任务 (Task) ======== */

const TASK_KEY = 'apix_tasks'

export function getTasks(): AgentTask[] {
    try { return JSON.parse(localStorage.getItem(TASK_KEY) || '[]') } catch { return [] }
}
export function saveTasks(list: AgentTask[]) {
    localStorage.setItem(TASK_KEY, JSON.stringify(list))
}

/* ======== 文件操作 ======== */

export async function uploadFile(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    const res = await fetch(`${API_BASE}/file/file/insert_file`, {
        method: 'POST',
        body: formData,
    })
    return res.json()
}

export async function getRecentFiles(limit = 20) {
    return authPost('/file/file/get_recent_files', { limit })
}

/* ======== 模型列表 ======== */

export async function fetchModelsList(provider: string, apiKey: string) {
    return authPost('/api/v1/get_models_list', {
        model_provider: provider,
        api_key: apiKey,
    })
}
