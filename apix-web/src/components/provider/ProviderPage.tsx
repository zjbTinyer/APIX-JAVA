import { useState, useEffect } from 'react'
import { getProviders, saveProviders, fetchModelsList } from '../../lib/api'
import type { Provider } from '../../lib/types'
import ProviderCard from './ProviderCard'
import ProviderEditDialog from './ProviderEditDialog'

export default function ProviderPage() {
    const [providers, setProviders] = useState<Provider[]>([])
    const [search, setSearch] = useState('')
    const [dialogVisible, setDialogVisible] = useState(false)
    const [editing, setEditing] = useState<Provider | null>(null)

    useEffect(() => { setProviders(getProviders()) }, [])

    const filtered = providers.filter((p) =>
        [p.name, p.endpoint, p.description].some((s) =>
            s?.toLowerCase().includes(search.toLowerCase())
        )
    )

    const save = (list: Provider[]) => {
        setProviders(list)
        saveProviders(list)
    }

    const handleSave = (provider: Provider) => {
        if (editing) {
            save(providers.map((p) => (p.provider_id === provider.provider_id ? provider : p)))
        } else {
            save([...providers, { ...provider, provider_id: `prov_${Date.now()}` }])
        }
        setDialogVisible(false)
        setEditing(null)
    }

    const handleDelete = (id: string) => {
        save(providers.filter((p) => p.provider_id !== id))
    }

    const handleToggle = (id: string, enabled: boolean) => {
        save(providers.map((p) => (p.provider_id === id ? { ...p, enabled } : p)))
    }

    return (
        <div className="p-6 max-w-5xl mx-auto">
            {/* 标题栏 */}
            <div className="mb-6">
                <div className="flex items-center justify-between mb-3">
                    <h1 className="text-xl font-bold text-gray-900">LLM 供应商</h1>
                    <div className="flex gap-2">
                        <button
                            onClick={() => { setEditing(null); setDialogVisible(true) }}
                            className="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 text-white text-sm font-medium rounded-lg transition"
                        >
                            + 新建供应商
                        </button>
                    </div>
                </div>

                <input
                    type="text"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    placeholder="通过供应商名称、endpoint、描述搜索..."
                    className="w-full max-w-md px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500"
                />
            </div>

            {/* 说明 */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 mb-6 text-xs text-blue-700 space-y-1">
                <p>1. 支持接入符合 OpenAI API 兼容协议的大模型服务。</p>
                <p>2. 请准备 API Endpoint 和 API Key，添加后建议测试连接。</p>
                <p>3. 部分第三方模型可能在响应速度或兼容性上存在差异。</p>
            </div>

            {/* 供应商列表 */}
            {filtered.length === 0 ? (
                <div className="text-center text-gray-400 py-20">暂无供应商</div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {filtered.map((p) => (
                        <ProviderCard
                            key={p.provider_id}
                            provider={p}
                            onToggle={handleToggle}
                            onDelete={handleDelete}
                            onEdit={() => { setEditing(p); setDialogVisible(true) }}
                        />
                    ))}
                </div>
            )}

            {/* 编辑对话框 */}
            {dialogVisible && (
                <ProviderEditDialog
                    provider={editing}
                    onSave={handleSave}
                    onClose={() => { setDialogVisible(false); setEditing(null) }}
                />
            )}
        </div>
    )
}
