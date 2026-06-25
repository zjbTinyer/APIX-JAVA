import { useState, useRef, useEffect, useCallback } from 'react'
import { useChatStore } from '../store/chatStore'
import type { UploadedFile } from '../lib/types'

interface Props {
    onSend: (content: string, files?: UploadedFile[]) => void
    disabled: boolean
    generating?: boolean
    onStop?: () => void
}

export default function ChatInput({ onSend, disabled, generating, onStop }: Props) {
    const { config, setConfig } = useChatStore()
    const [text, setText] = useState('')
    const [fullscreen, setFullscreen] = useState(false)
    const [files, setFiles] = useState<UploadedFile[]>([])
    const [showConfig, setShowConfig] = useState(false)
    const [dragOver, setDragOver] = useState(false)
    const textareaRef = useRef<HTMLTextAreaElement>(null)
    const fileInputRef = useRef<HTMLInputElement>(null)

    useEffect(() => {
        if (textareaRef.current && !fullscreen) {
            textareaRef.current.style.height = 'auto'
            textareaRef.current.style.height = Math.min(textareaRef.current.scrollHeight, fullscreen ? 400 : 200) + 'px'
        }
    }, [text, fullscreen])

    const handleSend = useCallback(() => {
        const trimmed = text.trim()
        if ((!trimmed && files.length === 0) || disabled || generating) return
        onSend(trimmed, files.length > 0 ? files : undefined)
        setText('')
        setFiles([])
    }, [text, files, disabled, generating, onSend])

    // 文件拖拽
    const onDrop = useCallback((e: React.DragEvent) => {
        e.preventDefault()
        setDragOver(false)
        const droppedFiles = Array.from(e.dataTransfer.files)
        const newFiles: UploadedFile[] = droppedFiles.map((f) => ({
            id: `file_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
            name: f.name,
            size: f.size,
            type: f.type,
            data: f,
        }))
        setFiles((prev) => [...prev, ...newFiles])
    }, [])

    // 粘贴
    const onPaste = useCallback((e: React.ClipboardEvent) => {
        const items = Array.from(e.clipboardData.items)
        const imageItems = items.filter((item) => item.type.startsWith('image/'))
        if (imageItems.length > 0) {
            e.preventDefault()
            imageItems.forEach((item) => {
                const blob = item.getAsFile()
                if (blob) {
                    setFiles((prev) => [...prev, {
                        id: `paste_${Date.now()}`,
                        name: `pasted_${Date.now()}.png`,
                        size: blob.size,
                        type: blob.type,
                        data: blob as File,
                    }])
                }
            })
        }
    }, [])

    const removeFile = (id: string) => setFiles((prev) => prev.filter((f) => f.id !== id))

    return (
        <div className={`border-t border-gray-200 bg-white ${fullscreen ? 'fixed inset-0 z-50 flex flex-col' : ''}`}>
            {/* 工具栏 */}
            <div className={`${fullscreen ? 'shrink-0' : ''}`}>
                <div className="max-w-4xl mx-auto px-4 pt-2 flex items-center gap-1.5 flex-wrap">
                    {/* 供应商选择 */}
                    <select
                        value={config.modelsProvider}
                        onChange={(e) => { setConfig({ modelsProvider: e.target.value, modelName: '' }) }}
                        className={`px-2 py-1 border rounded-lg text-xs outline-none focus:ring-1 focus:ring-indigo-500 ${!config.apiKey ? 'border-red-300 bg-red-50' : 'border-gray-200 bg-white text-gray-600'}`}
                        title="供应商"
                    >
                        <option value="">🌐 供应商</option>
                        <option value="openai">🔵 OpenAI</option>
                        <option value="deepseek">🟢 DeepSeek</option>
                        <option value="moonshot">🟣 Moonshot</option>
                        <option value="ollama:local">⚪ Ollama</option>
                    </select>

                    {/* API Key (错误状态) */}
                    <input
                        value={config.apiKey}
                        onChange={(e) => setConfig({ apiKey: e.target.value })}
                        placeholder="API Key"
                        className={`w-20 px-2 py-1 border rounded-lg text-xs outline-none focus:ring-1 focus:ring-indigo-500 ${!config.apiKey && config.modelsProvider ? 'border-red-300 bg-red-50 placeholder-red-300' : 'border-gray-200 text-gray-600'}`}
                    />

                    {/* 深度思考 */}
                    <button
                        onClick={() => fileInputRef.current?.click()}
                        className="px-2 py-1 bg-gray-100 hover:bg-gray-200 rounded-lg text-xs text-gray-600 transition"
                    >
                        📎 文件
                    </button>
                    <input ref={fileInputRef} type="file" multiple className="hidden" onChange={(e) => {
                        const selected = Array.from(e.target.files || [])
                        setFiles((prev) => [...prev, ...selected.map((f) => ({ id: `upload_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`, name: f.name, size: f.size, type: f.type, data: f }))])
                        e.target.value = ''
                    }} />

                    {/* 全屏 */}
                    <button
                        onClick={() => setFullscreen(!fullscreen)}
                        className={`px-2 py-1 rounded-lg text-xs transition ${fullscreen ? 'bg-indigo-100 text-indigo-600' : 'bg-gray-100 text-gray-500'}`}
                    >
                        {fullscreen ? '⬇ 还原' : '⬆ 全屏'}
                    </button>

                    <div className="flex-1" />

                    {/* 快捷配置按钮 */}
                    <button onClick={() => setShowConfig(!showConfig)} className="px-2 py-1 text-xs text-gray-400 hover:text-gray-600">
                        {showConfig ? '收起' : '更多'}
                    </button>
                </div>

                {/* 展开配置 */}
                {showConfig && (
                    <div className="max-w-4xl mx-auto px-4 py-2 flex flex-wrap gap-2">
                        <label className="flex items-center gap-1 text-xs text-gray-500">
                            <input type="checkbox" checked={config.enableFileOperation} onChange={(e) => setConfig({ enableFileOperation: e.target.checked })} className="rounded" />
                            文件操作
                        </label>
                        <label className="flex items-center gap-1 text-xs text-gray-500">
                            <input type="checkbox" checked={config.enableWebSearch} onChange={(e) => setConfig({ enableWebSearch: e.target.checked })} className="rounded" />
                            网络搜索
                        </label>
                        <label className="flex items-center gap-1 text-xs text-gray-500">
                            <input type="checkbox" checked={config.enableKnowledgeRetrieval} onChange={(e) => setConfig({ enableKnowledgeRetrieval: e.target.checked })} className="rounded" />
                            知识库
                        </label>
                        <label className="flex items-center gap-1 text-xs text-gray-500">
                            <input type="checkbox" checked={config.enableCommandOperation} onChange={(e) => setConfig({ enableCommandOperation: e.target.checked })} className="rounded" />
                            命令执行
                        </label>
                    </div>
                )}
            </div>

            {/* 已上传文件标签 */}
            {files.length > 0 && (
                <div className={`max-w-4xl mx-auto px-4 py-1 flex flex-wrap gap-1 ${fullscreen ? 'shrink-0' : ''}`}>
                    {files.map((f) => (
                        <span key={f.id} className="inline-flex items-center gap-1 px-2 py-0.5 bg-indigo-50 text-indigo-600 text-xs rounded-full">
                            📄 {f.name}
                            <button onClick={() => removeFile(f.id)} className="text-indigo-400 hover:text-indigo-600 ml-0.5">✕</button>
                        </span>
                    ))}
                </div>
            )}

            {/* 拖拽浮层 */}
            {dragOver && (
                <div className="max-w-4xl mx-auto px-4 py-8 border-2 border-dashed border-indigo-300 rounded-xl bg-indigo-50/50 text-center text-sm text-indigo-500">
                    释放以上传文件
                </div>
            )}

            {/* 输入区 */}
            <div
                className={`max-w-4xl mx-auto w-full ${fullscreen ? 'flex-1 flex flex-col' : ''}`}
                onDragOver={(e) => { e.preventDefault(); setDragOver(true) }}
                onDragLeave={() => setDragOver(false)}
                onDrop={onDrop}
            >
                <div className={`flex items-end gap-2 bg-gray-50 rounded-xl border border-gray-200 px-4 py-2 focus-within:ring-2 focus-within:ring-indigo-500 focus-within:border-indigo-500 transition m-2 ${fullscreen ? 'flex-1' : ''}`}>
                    <textarea
                        ref={textareaRef}
                        value={text}
                        onChange={(e) => setText(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend() }
                        }}
                        onPaste={onPaste}
                        placeholder={files.length > 0 ? '添加描述或直接发送...' : '输入消息，Enter 发送，Shift+Enter 换行，粘贴/拖拽上传文件'}
                        className={`flex-1 bg-transparent outline-none resize-none text-sm py-1 ${fullscreen ? 'min-h-[200px]' : 'max-h-[200px]'}`}
                        disabled={disabled}
                    />
                    {generating ? (
                        <button onClick={onStop} className="shrink-0 px-4 py-2 bg-red-500 hover:bg-red-600 text-white text-xs font-medium rounded-lg transition relative overflow-hidden">
                            <span className="relative z-10">⏹ 停止</span>
                            <span className="absolute inset-0">
                                <span className="absolute inset-0 bg-red-400/50 animate-ping rounded-lg" style={{ animationDuration: '1.5s' }} />
                                <span className="absolute inset-0 bg-red-300/30 animate-ping rounded-lg" style={{ animationDuration: '2s', animationDelay: '0.3s' }} />
                                <span className="absolute inset-0 bg-red-200/20 animate-ping rounded-lg" style={{ animationDuration: '2.5s', animationDelay: '0.6s' }} />
                            </span>
                        </button>
                    ) : (
                        <button
                            onClick={handleSend}
                            disabled={disabled || (!text.trim() && files.length === 0)}
                            className="shrink-0 px-4 py-2 bg-indigo-500 hover:bg-indigo-600 disabled:bg-gray-300 text-white text-xs font-medium rounded-lg transition"
                        >
                            发送
                        </button>
                    )}
                </div>
                <p className="text-xs text-gray-400 text-center pb-2">
                    {generating ? 'AI 正在生成回复...' : `Enter 发送 · Shift+Enter 换行 · ${files.length > 0 ? `${files.length} 个文件待发送` : '拖拽或粘贴文件上传'}`}
                </p>
            </div>
        </div>
    )
}
