import { useState, useCallback } from 'react'

interface Props {
    onQuote?: (text: string) => void
}

/**
 * 文本选中浮动气泡 — 包裹需要监听选中的区域
 */
export default function SelectionBubble({ onQuote }: Props) {
    const [visible, setVisible] = useState(false)
    const [pos, setPos] = useState({ x: 0, y: 0 })
    const [selectedText, setSelectedText] = useState('')

    const handleMouseUp = useCallback((e: React.MouseEvent) => {
        const sel = window.getSelection()
        const text = sel?.toString().trim() || ''
        if (text && text.length > 1 && text.length < 500) {
            const range = sel?.getRangeAt(0)
            const rect = range?.getBoundingClientRect()
            if (rect) {
                setPos({ x: rect.left + rect.width / 2, y: rect.top - 8 })
                setSelectedText(text)
                setVisible(true)
                return
            }
        }
        setVisible(false)
    }, [])

    const handleCopy = useCallback(() => {
        navigator.clipboard.writeText(selectedText)
        setVisible(false)
    }, [selectedText])

    const handleQuote = useCallback(() => {
        onQuote?.(selectedText)
        setVisible(false)
    }, [selectedText, onQuote])

    if (!visible) return null

    return (
        <div
            className="fixed z-[100] flex gap-1 bg-gray-900 text-white rounded-lg px-2 py-1 shadow-lg"
            style={{ left: pos.x, top: pos.y, transform: 'translate(-50%, -100%)' }}
            onMouseDown={(e) => e.preventDefault()}
        >
            <button onClick={handleCopy} className="px-2 py-0.5 text-xs hover:bg-gray-700 rounded transition">📋 复制</button>
            {onQuote && <button onClick={handleQuote} className="px-2 py-0.5 text-xs hover:bg-gray-700 rounded transition">💬 引用</button>}
        </div>
    )
}

/**
 * 包裹内容让其支持选中气泡
 */
export function WithSelection({ children, onQuote }: { children: React.ReactNode; onQuote?: (text: string) => void }) {
    return (
        <div onMouseUp={(e) => {
            // 延迟执行让 selection 稳定
            setTimeout(() => {
                const sel = window.getSelection()
                const text = sel?.toString().trim() || ''
                if (text && text.length > 1 && text.length < 500) {
                    const range = sel?.getRangeAt(0)
                    const rect = range?.getBoundingClientRect()
                    if (rect) {
                        // 创建临时气泡
                        const el = document.createElement('div')
                        el.className = 'fixed z-[100] flex gap-1 bg-gray-900 text-white rounded-lg px-2 py-1 shadow-lg'
                        el.style.left = `${rect.left + rect.width / 2}px`
                        el.style.top = `${rect.top - 8}px`
                        el.style.transform = 'translate(-50%, -100%)'

                        const copyBtn = document.createElement('button')
                        copyBtn.className = 'px-2 py-0.5 text-xs hover:bg-gray-700 rounded transition'
                        copyBtn.textContent = '📋 复制'
                        copyBtn.onclick = () => { navigator.clipboard.writeText(text); el.remove() }

                        const quoteBtn = document.createElement('button')
                        quoteBtn.className = 'px-2 py-0.5 text-xs hover:bg-gray-700 rounded transition'
                        quoteBtn.textContent = '💬 引用'
                        quoteBtn.onclick = () => { onQuote?.(text); el.remove() }

                        el.appendChild(copyBtn)
                        if (onQuote) el.appendChild(quoteBtn)
                        document.body.appendChild(el)

                        const remove = () => { el.remove(); document.removeEventListener('mousedown', remove) }
                        setTimeout(() => document.addEventListener('mousedown', remove), 0)
                    }
                }
            }, 10)
        }}>
            {children}
        </div>
    )
}
