import { useState } from 'react'

interface Props {
    value: string
    onChange?: (value: string) => void
    language?: string
    readOnly?: boolean
    fileName?: string
}

export default function CodeEditor({ value, onChange, language, readOnly, fileName }: Props) {
    const [showSearch, setShowSearch] = useState(false)
    const [searchQuery, setSearchQuery] = useState('')

    const langLabel: Record<string, string> = {
        md: 'Markdown', py: 'Python', js: 'JavaScript', ts: 'TypeScript',
        json: 'JSON', html: 'HTML', css: 'CSS', txt: 'Text',
    }

    return (
        <div className="h-full flex flex-col bg-gray-900">
            {/* 工具栏 */}
            <div className="flex items-center justify-between px-3 py-1.5 bg-gray-800 border-b border-gray-700 shrink-0">
                <span className="text-xs text-gray-400">
                    {fileName && <span className="text-gray-300 mr-2">{fileName}</span>}
                    {language && <span className="px-1.5 py-0.5 bg-gray-700 rounded text-xs text-gray-400">{langLabel[language] || language}</span>}
                </span>
                <button
                    onClick={() => setShowSearch(!showSearch)}
                    className="text-xs text-gray-400 hover:text-gray-200 px-2 py-0.5 hover:bg-gray-700 rounded"
                >
                    {showSearch ? '关闭搜索' : '搜索'}
                </button>
            </div>

            {/* 搜索栏 */}
            {showSearch && (
                <div className="px-3 py-1.5 bg-gray-800 border-b border-gray-700">
                    <input
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder="搜索..."
                        className="w-full px-2 py-1 bg-gray-700 border border-gray-600 rounded text-xs text-gray-200 outline-none focus:ring-1 focus:ring-indigo-500"
                        autoFocus
                    />
                </div>
            )}

            {/* 编辑器 */}
            <textarea
                value={value}
                onChange={(e) => onChange?.(e.target.value)}
                readOnly={readOnly}
                className="flex-1 bg-gray-900 text-gray-100 p-4 text-sm font-mono leading-relaxed outline-none resize-none"
                spellCheck={false}
            />

            {/* 提示 */}
            <div className="px-3 py-1 bg-gray-800 border-t border-gray-700 text-xs text-gray-500 shrink-0">
                专业代码编辑建议使用 VS Code
            </div>
        </div>
    )
}
