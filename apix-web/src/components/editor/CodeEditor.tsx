import { useState, useMemo, useRef, useCallback } from 'react'

interface Props {
    value: string
    onChange?: (value: string) => void
    language?: string
    readOnly?: boolean
    fileName?: string
}

// 简单语法高亮 — 关键词映射
const HIGHLIGHT_RULES: Record<string, RegExp[]> = {
    js: [/\b(const|let|var|function|return|import|export|from|if|else|for|while|async|await|class|new|this|typeof|throw|try|catch)\b/g],
    ts: [/\b(const|let|var|function|return|import|export|from|if|else|for|while|async|await|class|interface|type|enum|new|this|typeof|throw|try|catch|implements|extends)\b/g],
    py: [/\b(def|class|return|import|from|if|elif|else|for|while|try|except|finally|with|as|async|await|pass|None|True|False|yield|lambda|print)\b/g],
    json: [/"[^"]*"\s*:/g],
    html: [/(<\/?[a-zA-Z][^>]*>)/g],
    css: [/([a-zA-Z-]+)\s*:/g],
    md: [/(#{1,6}\s.*)/g, /(`{1,3}[^`]+`{1,3})/g],
}

function highlightCode(code: string, lang: string): React.ReactNode[] {
    const rules = HIGHLIGHT_RULES[lang] || []
    if (!rules.length) return [code]

    // 组合所有规则找出所有匹配位置
    const matches: { start: number; end: number; text: string }[] = []
    rules.forEach((regex) => {
        let m: RegExpExecArray | null
        while ((m = regex.exec(code)) !== null) {
            matches.push({ start: m.index, end: m.index + m[0].length, text: m[0] })
        }
    })
    matches.sort((a, b) => a.start - b.start)

    if (!matches.length) return [code]

    const result: React.ReactNode[] = []
    let lastEnd = 0
    matches.forEach((m, i) => {
        if (m.start > lastEnd) {
            result.push(<span key={`t${i}_pre`}>{code.slice(lastEnd, m.start)}</span>)
        }
        result.push(<span key={`m${i}`} className="text-indigo-300">{m.text}</span>)
        lastEnd = m.end
    })
    if (lastEnd < code.length) {
        result.push(<span key="last">{code.slice(lastEnd)}</span>)
    }
    return result
}

// 简单的行号组件
function LineNumbers({ code }: { code: string }) {
    const lines = code.split('\n').length
    return (
        <div className="select-none text-right pr-3 text-gray-600 text-xs leading-relaxed py-3 font-mono border-r border-gray-800 mr-3">
            {Array.from({ length: Math.max(lines, 1) }, (_, i) => (
                <div key={i} className="leading-relaxed">{i + 1}</div>
            ))}
        </div>
    )
}

export default function CodeEditor({ value, onChange, language, readOnly, fileName }: Props) {
    const [showSearch, setShowSearch] = useState(false)
    const [searchQuery, setSearchQuery] = useState('')
    const [showMinimap, setShowMinimap] = useState(false)
    const textareaRef = useRef<HTMLTextAreaElement>(null)

    const langLabel: Record<string, string> = {
        md: 'Markdown', py: 'Python', js: 'JavaScript', ts: 'TypeScript',
        json: 'JSON', html: 'HTML', css: 'CSS', txt: 'Text',
        aflow: '工作流',
    }

    const lang = language || 'txt'
    const isEditable = !readOnly && lang !== 'aflow'

    const highlightedLines = useMemo(() => {
        if (lang === 'txt') return null
        return value.split('\n').map((line, i) => (
            <div key={i} className="leading-relaxed whitespace-pre">
                {highlightCode(line, lang)}
            </div>
        ))
    }, [value, lang])

    // 搜索高亮
    const searchMatches = useMemo(() => {
        if (!searchQuery) return []
        const indices: number[] = []
        let idx = value.indexOf(searchQuery)
        while (idx !== -1) {
            indices.push(idx)
            idx = value.indexOf(searchQuery, idx + 1)
        }
        return indices
    }, [searchQuery, value])

    const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
        // Tab 插入空格
        if (e.key === 'Tab') {
            e.preventDefault()
            const ta = textareaRef.current
            if (!ta) return
            const start = ta.selectionStart
            const end = ta.selectionEnd
            const newVal = value.substring(0, start) + '    ' + value.substring(end)
            onChange?.(newVal)
            // 恢复光标位置
            requestAnimationFrame(() => {
                ta.selectionStart = ta.selectionEnd = start + 4
            })
        }
    }, [value, onChange])

    return (
        <div className="h-full flex flex-col bg-gray-900">
            {/* 工具栏 */}
            <div className="flex items-center justify-between px-3 py-1.5 bg-gray-800/80 border-b border-gray-700/50 shrink-0">
                <div className="flex items-center gap-2">
                    {fileName && <span className="text-xs text-gray-300 font-medium">{fileName}</span>}
                    {lang !== 'txt' && (
                        <span className="px-1.5 py-0.5 bg-indigo-500/20 text-indigo-300 rounded text-xs font-mono">
                            {langLabel[lang] || lang}
                        </span>
                    )}
                </div>
                <div className="flex items-center gap-1">
                    <button
                        onClick={() => setShowMinimap(!showMinimap)}
                        className={`text-xs px-2 py-0.5 rounded transition ${showMinimap ? 'bg-indigo-500/20 text-indigo-300' : 'text-gray-500 hover:text-gray-300 hover:bg-gray-700'}`}
                        title="小地图"
                    >
                        <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2" /><line x1="3" y1="9" x2="21" y2="9" /><line x1="9" y1="3" x2="9" y2="21" /></svg>
                    </button>
                    <button
                        onClick={() => setShowSearch(!showSearch)}
                        className={`text-xs px-2 py-0.5 rounded transition ${showSearch ? 'bg-indigo-500/20 text-indigo-300' : 'text-gray-500 hover:text-gray-300 hover:bg-gray-700'}`}
                        title="搜索"
                    >
                        <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></svg>
                    </button>
                </div>
            </div>

            {/* 搜索栏 */}
            {showSearch && (
                <div className="px-3 py-1.5 bg-gray-800 border-b border-gray-700/50 flex items-center gap-2">
                    <svg className="w-3.5 h-3.5 text-gray-500 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" /></svg>
                    <input
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder="搜索..."
                        className="flex-1 px-2 py-1 bg-gray-700/50 border border-gray-600 rounded text-xs text-gray-200 outline-none focus:ring-1 focus:ring-indigo-500 placeholder-gray-500"
                        autoFocus
                    />
                    {searchQuery && (
                        <span className="text-xs text-gray-500">{searchMatches.length} 个结果</span>
                    )}
                </div>
            )}

            {/* 编辑器主体 */}
            <div className="flex-1 flex min-h-0 relative">
                {/* 行号 + 代码 */}
                <div className="flex-1 flex overflow-auto">
                    <LineNumbers code={value} />
                    <div className="relative flex-1 min-w-0">
                        {/* 高亮层（只读时或始终显示） */}
                        {highlightedLines && !isEditable && (
                            <div className="absolute inset-0 p-3 text-sm font-mono leading-relaxed text-gray-100 pointer-events-none overflow-hidden">
                                {highlightedLines}
                            </div>
                        )}
                        {/* Textarea 编辑层 */}
                        {isEditable ? (
                            <textarea
                                ref={textareaRef}
                                value={value}
                                onChange={(e) => onChange?.(e.target.value)}
                                onKeyDown={handleKeyDown}
                                className={`w-full h-full bg-transparent text-gray-100 p-3 text-sm font-mono leading-relaxed outline-none resize-none ${highlightedLines ? 'text-transparent caret-gray-100' : ''
                                    }`}
                                spellCheck={false}
                                autoComplete="off"
                                autoCorrect="off"
                                autoCapitalize="off"
                                wrap="off"
                            />
                        ) : (
                            <div className="p-3 text-sm font-mono leading-relaxed text-gray-100 h-full overflow-auto whitespace-pre">
                                {highlightedLines || value}
                            </div>
                        )}
                    </div>
                </div>

                {/* 小地图 */}
                {showMinimap && (
                    <div className="w-24 border-l border-gray-800 bg-gray-900/80 overflow-hidden shrink-0">
                        <div className="text-[4px] leading-[4px] font-mono text-gray-600 p-1 break-all select-none"
                            style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}
                        >
                            {value.split('\n').slice(0, 200).map((line, i) => (
                                <div key={i}>{line || ' '}</div>
                            ))}
                        </div>
                    </div>
                )}
            </div>

            {/* 状态栏 */}
            <div className="flex items-center justify-between px-3 py-1 bg-gray-800/80 border-t border-gray-700/50 text-xs text-gray-500 shrink-0">
                <span>
                    {value.split('\n').length} 行 · {value.length} 字符
                </span>
                <span className="text-gray-600">
                    {langLabel[lang] || lang} · Tab: 4空格
                </span>
            </div>
        </div>
    )
}
