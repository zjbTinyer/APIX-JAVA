import { useState, useCallback, useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'

interface Props {
    onQuote?: (text: string) => void
}

/**
 * 文本选中浮动气泡 — 使用 React Portal 确保正确层级
 */
export default function SelectionBubble({ onQuote }: Props) {
    const [visible, setVisible] = useState(false)
    const [pos, setPos] = useState({ x: 0, y: 0 })
    const [selectedText, setSelectedText] = useState('')
    const bubbleRef = useRef<HTMLDivElement>(null)

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

    // 点击外部关闭
    useEffect(() => {
        if (!visible) return
        const handleClick = (e: MouseEvent) => {
            if (bubbleRef.current && !bubbleRef.current.contains(e.target as Node)) {
                setVisible(false)
            }
        }
        // 延迟添加避免触发当前 mouseup
        const timer = setTimeout(() => document.addEventListener('mousedown', handleClick), 0)
        return () => { clearTimeout(timer); document.removeEventListener('mousedown', handleClick) }
    }, [visible])

    const handleCopy = useCallback(() => {
        navigator.clipboard.writeText(selectedText)
        setVisible(false)
    }, [selectedText])

    const handleQuote = useCallback(() => {
        onQuote?.(selectedText)
        setVisible(false)
    }, [selectedText, onQuote])

    return createPortal(
        <div
            ref={bubbleRef}
            className={`fixed z-[100] flex gap-1 bg-gray-900 dark:bg-gray-800 text-white rounded-xl px-2 py-1.5 shadow-xl border border-gray-700/50 ${visible ? 'opacity-100 scale-100' : 'opacity-0 scale-95 pointer-events-none'
                } transition-all duration-150 ease-out`}
            style={{
                left: pos.x,
                top: pos.y,
                transform: 'translate(-50%, -100%)',
            }}
            onMouseDown={(e) => e.preventDefault()}
        >
            <button
                onClick={handleCopy}
                className="px-2.5 py-1 text-xs hover:bg-gray-700 rounded-lg transition flex items-center gap-1"
            >
                <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                </svg>
                复制
            </button>
            {onQuote && (
                <button
                    onClick={handleQuote}
                    className="px-2.5 py-1 text-xs hover:bg-gray-700 rounded-lg transition flex items-center gap-1"
                >
                    <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                    </svg>
                    引用
                </button>
            )}
        </div>,
        document.body
    )
}

/**
 * 包裹内容让其支持选中气泡（使用 React Portal）
 */
export function WithSelection({ children, onQuote }: { children: React.ReactNode; onQuote?: (text: string) => void }) {
    const [bubble, setBubble] = useState<{ visible: boolean; pos: { x: number; y: number }; text: string } | null>(null)
    const timerRef = useRef<ReturnType<typeof setTimeout>>()

    const handleMouseUp = useCallback(() => {
        clearTimeout(timerRef.current)
        timerRef.current = setTimeout(() => {
            const sel = window.getSelection()
            const text = sel?.toString().trim() || ''
            if (text && text.length > 1 && text.length < 500) {
                const range = sel?.getRangeAt(0)
                const rect = range?.getBoundingClientRect()
                if (rect) {
                    setBubble({ visible: true, pos: { x: rect.left + rect.width / 2, y: rect.top - 8 }, text })
                    return
                }
            }
            setBubble(null)
        }, 10)
    }, [])

    // 点击外部关闭
    useEffect(() => {
        if (!bubble?.visible) return
        const handleClick = () => setBubble(null)
        const timer = setTimeout(() => document.addEventListener('mousedown', handleClick), 0)
        return () => { clearTimeout(timer); document.removeEventListener('mousedown', handleClick) }
    }, [bubble])

    return (
        <>
            <div onMouseUp={handleMouseUp}>{children}</div>
            {bubble?.visible && createPortal(
                <div
                    className="fixed z-[100] flex gap-1 bg-gray-900 dark:bg-gray-800 text-white rounded-xl px-2 py-1.5 shadow-xl border border-gray-700/50 animate-scale-in"
                    style={{ left: bubble.pos.x, top: bubble.pos.y, transform: 'translate(-50%, -100%)' }}
                    onMouseDown={(e) => e.preventDefault()}
                >
                    <button
                        onClick={() => { navigator.clipboard.writeText(bubble.text); setBubble(null) }}
                        className="px-2.5 py-1 text-xs hover:bg-gray-700 rounded-lg transition flex items-center gap-1"
                    >
                        <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2" /><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" /></svg>
                        复制
                    </button>
                    {onQuote && (
                        <button
                            onClick={() => { onQuote(bubble.text); setBubble(null) }}
                            className="px-2.5 py-1 text-xs hover:bg-gray-700 rounded-lg transition flex items-center gap-1"
                        >
                            <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>
                            引用
                        </button>
                    )}
                </div>,
                document.body
            )}
        </>
    )
}
