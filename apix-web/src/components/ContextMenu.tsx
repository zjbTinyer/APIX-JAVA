import { useEffect, useRef } from 'react'

interface MenuItem {
    label: string
    icon?: string
    onClick: () => void
    danger?: boolean
    divider?: boolean
}

interface Props {
    x: number
    y: number
    items: MenuItem[]
    onClose: () => void
}

export default function ContextMenu({ x, y, items, onClose }: Props) {
    const ref = useRef<HTMLDivElement>(null)

    useEffect(() => {
        const handler = (e: MouseEvent) => {
            if (ref.current && !ref.current.contains(e.target as Node)) onClose()
        }
        document.addEventListener('mousedown', handler)
        return () => document.removeEventListener('mousedown', handler)
    }, [onClose])

    // 确保不超出屏幕
    const adjustedX = Math.min(x, window.innerWidth - 180)
    const adjustedY = Math.min(y, window.innerHeight - items.length * 36 - 16)

    return (
        <div
            ref={ref}
            className="fixed z-[100] bg-white rounded-xl shadow-xl border border-gray-200 py-1 w-44"
            style={{ left: adjustedX, top: adjustedY }}
        >
            {items.map((item, i) => (
                item.divider ? (
                    <hr key={i} className="my-1 border-gray-100" />
                ) : (
                    <button
                        key={i}
                        onClick={() => { item.onClick(); onClose() }}
                        className={`w-full text-left px-3 py-2 text-sm flex items-center gap-2 transition ${item.danger ? 'text-red-500 hover:bg-red-50' : 'text-gray-700 hover:bg-gray-50'
                            }`}
                    >
                        {item.icon && <span>{item.icon}</span>}
                        {item.label}
                    </button>
                )
            ))}
        </div>
    )
}
