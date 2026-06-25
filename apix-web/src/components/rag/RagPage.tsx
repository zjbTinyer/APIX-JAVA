import { useState, useEffect } from 'react'
import { getRagDocuments, saveRagDocuments } from '../../lib/api'
import type { RagDocument } from '../../lib/types'
import RagEditDialog from './RagEditDialog'

export default function RagPage() {
    const [docs, setDocs] = useState<RagDocument[]>([])
    const [search, setSearch] = useState('')
    const [editingDoc, setEditingDoc] = useState<RagDocument | null>(null)

    useEffect(() => { setDocs(getRagDocuments()) }, [])

    const filtered = docs.filter((d) =>
        [d.name, d.description].some((s) => s?.toLowerCase().includes(search.toLowerCase()))
    )

    const save = (list: RagDocument[]) => { setDocs(list); saveRagDocuments(list) }

    const handleUpload = () => {
        const input = document.createElement('input')
        input.type = 'file'
        input.accept = '.md,.txt,.json,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx'
        input.onchange = (e) => {
            const file = (e.target as HTMLInputElement).files?.[0]
            if (!file) return
            const doc: RagDocument = {
                doc_id: `doc_${Date.now()}`,
                name: file.name,
                description: '',
                file_type: file.name.split('.').pop() || 'unknown',
                file_size: file.size,
                status: 'indexing',
                enabled: true,
                created_at: new Date().toISOString(),
            }
            save([...docs, doc])
            // 模拟索引完成
            setTimeout(() => {
                save(docs.map((d) => d.doc_id === doc.doc_id ? { ...d, status: 'indexed' as const } : d))
            }, 2000)
        }
        input.click()
    }

    const formatSize = (bytes: number) => {
        if (bytes < 1024) return `${bytes}B`
        if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`
        return `${(bytes / 1024 / 1024).toFixed(1)}MB`
    }

    return (
        <div className="p-6 max-w-5xl mx-auto">
            <div className="mb-6">
                <div className="flex items-center justify-between mb-3">
                    <h1 className="text-xl font-bold text-gray-900">RAG 知识库</h1>
                    <button onClick={handleUpload} className="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 text-white text-sm font-medium rounded-lg transition">
                        + 上传文档
                    </button>
                </div>
                <input
                    type="text"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    placeholder="通过文档名称、描述搜索..."
                    className="w-full max-w-md px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500"
                />
            </div>

            <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 mb-6 text-xs text-amber-700 space-y-1">
                <p>1. 推荐 Markdown、TXT、JSON 格式，单文件不超过 20MB。</p>
                <p>2. 仅支持通过 Ollama 部署的 Embedding 模型。更换模型后需重新索引。</p>
                <p>3. 仅启用状态的文档会加载给 Agent 检索。</p>
            </div>

            {filtered.length === 0 ? (
                <div className="text-center text-gray-400 py-20">暂无文档</div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {filtered.map((doc) => (
                        <div key={doc.doc_id} className="bg-white rounded-xl border border-gray-200 p-4 hover:shadow-md transition">
                            <div className="flex items-start justify-between mb-2">
                                <div className="flex-1 min-w-0">
                                    <h3 className="font-medium text-gray-900 truncate">{doc.name}</h3>
                                    <p className="text-xs text-gray-500 mt-0.5">{formatSize(doc.file_size)} · {doc.file_type}</p>
                                </div>
                                <label className="relative inline-flex items-center cursor-pointer ml-2 shrink-0">
                                    <input
                                        type="checkbox"
                                        checked={doc.enabled}
                                        onChange={(e) => save(docs.map((d) => d.doc_id === doc.doc_id ? { ...d, enabled: e.target.checked } : d))}
                                        className="sr-only peer"
                                    />
                                    <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-indigo-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-indigo-500" />
                                </label>
                            </div>
                            {doc.description && <p className="text-xs text-gray-500 mb-2">{doc.description}</p>}
                            <div className="flex items-center gap-2">
                                <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs ${doc.status === 'indexed' ? 'bg-green-100 text-green-700' :
                                    doc.status === 'indexing' ? 'bg-yellow-100 text-yellow-700' :
                                        'bg-red-100 text-red-700'
                                    }`}>
                                    {doc.status === 'indexed' ? '已索引' : doc.status === 'indexing' ? '索引中...' : '失败'}
                                </span>
                                <div className="flex items-center gap-2 ml-auto">
                                    <button
                                        onClick={() => setEditingDoc(doc)}
                                        className="text-xs text-indigo-400 hover:text-indigo-600"
                                    >
                                        编辑
                                    </button>
                                    <button
                                        onClick={() => save(docs.filter((d) => d.doc_id !== doc.doc_id))}
                                        className="text-xs text-red-400 hover:text-red-600"
                                    >
                                        删除
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* 编辑弹窗 */}
            {editingDoc && (
                <RagEditDialog
                    doc={editingDoc}
                    onSave={(updated) => {
                        save(docs.map((d) => d.doc_id === updated.doc_id ? updated : d))
                        setEditingDoc(null)
                    }}
                    onClose={() => setEditingDoc(null)}
                />
            )}
        </div>
    )
}