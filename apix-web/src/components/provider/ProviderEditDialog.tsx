import { useState } from 'react'
import type { Provider } from '../../lib/types'

interface Props {
    provider: Provider | null
    onSave: (provider: Provider) => void
    onClose: () => void
}

export default function ProviderEditDialog({ provider, onSave, onClose }: Props) {
    const [form, setForm] = useState<Provider>(
        provider || {
            provider_id: '',
            name: '',
            endpoint: '',
            api_key: '',
            type: 'custom',
            description: '',
            model_list: [],
            enabled: true,
            updated_at: new Date().toISOString(),
        }
    )

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault()
        onSave({ ...form, updated_at: new Date().toISOString() })
    }

    return (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={onClose}>
            <div className="bg-white rounded-xl shadow-xl w-full max-w-md mx-4 p-6" onClick={(e) => e.stopPropagation()}>
                <h2 className="text-lg font-bold text-gray-900 mb-4">
                    {provider ? '编辑供应商' : '新建供应商'}
                </h2>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm text-gray-600 mb-1">名称 *</label>
                        <input
                            required
                            value={form.name}
                            onChange={(e) => setForm({ ...form, name: e.target.value })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500"
                            placeholder="如: My OpenAI"
                        />
                    </div>

                    <div>
                        <label className="block text-sm text-gray-600 mb-1">Endpoint *</label>
                        <input
                            required
                            value={form.endpoint}
                            onChange={(e) => setForm({ ...form, endpoint: e.target.value })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500"
                            placeholder="https://api.openai.com/v1"
                        />
                    </div>

                    <div>
                        <label className="block text-sm text-gray-600 mb-1">API Key</label>
                        <input
                            type="password"
                            value={form.api_key}
                            onChange={(e) => setForm({ ...form, api_key: e.target.value })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500"
                            placeholder="sk-..."
                        />
                    </div>

                    <div>
                        <label className="block text-sm text-gray-600 mb-1">类型</label>
                        <select
                            value={form.type}
                            onChange={(e) => setForm({ ...form, type: e.target.value })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500"
                        >
                            <option value="custom">自定义</option>
                            <option value="openai">OpenAI</option>
                            <option value="deepseek">DeepSeek</option>
                            <option value="moonshot">Moonshot</option>
                            <option value="ollama">Ollama</option>
                        </select>
                    </div>

                    <div>
                        <label className="block text-sm text-gray-600 mb-1">描述</label>
                        <textarea
                            value={form.description}
                            onChange={(e) => setForm({ ...form, description: e.target.value })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500"
                            rows={2}
                            placeholder="可选描述信息"
                        />
                    </div>

                    <div className="flex justify-end gap-3 pt-2">
                        <button type="button" onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:bg-gray-100 rounded-lg transition">
                            取消
                        </button>
                        <button type="submit" className="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 text-white text-sm font-medium rounded-lg transition">
                            保存
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}
