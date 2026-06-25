import { useState } from 'react'
import type { RagDocument } from '../../lib/types'

interface Props {
    doc: RagDocument
    onSave: (doc: RagDocument) => void
    onClose: () => void
}

export default function RagEditDialog({ doc, onSave, onClose }: Props) {
    const [description, setDescription] = useState(doc.description)

    const charCount = description.length
    const estTokens = Math.ceil(charCount / 3)

    return (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={onClose}>
            <div className="bg-white rounded-xl shadow-xl w-full max-w-md mx-4 p-5" onClick={(e) => e.stopPropagation()}>
                <h2 className="text-lg font-bold text-gray-900 mb-4">编辑文档</h2>

                <div className="space-y-3 text-sm">
                    <div>
                        <label className="text-gray-500 block mb-1">文档名称</label>
                        <p className="text-gray-900 font-medium">{doc.name}</p>
                    </div>
                    <div>
                        <label className="text-gray-500 block mb-1">描述</label>
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value.slice(0, 500))}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
                            rows={4}
                            maxLength={500}
                            placeholder="添加文档描述以帮助索引..."
                        />
                        <div className="flex justify-between text-xs text-gray-400 mt-1">
                            <span>{charCount}/500 字符</span>
                            <span>约 {estTokens} tokens</span>
                        </div>
                    </div>
                </div>

                <div className="flex justify-end gap-3 mt-6">
                    <button onClick={onClose} className="px-4 py-2 text-sm text-gray-600 hover:bg-gray-100 rounded-lg transition">取消</button>
                    <button
                        onClick={() => onSave({ ...doc, description })}
                        className="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 text-white text-sm font-medium rounded-lg transition"
                    >
                        保存
                    </button>
                </div>
            </div>
        </div>
    )
}
