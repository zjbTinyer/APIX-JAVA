import { useMemo } from 'react'
import type { ChatMessage } from '../lib/types'

interface Props {
    messages: ChatMessage[]
    onJumpTo?: (index: number) => void
}

export default function MessageScrollBar({ messages, onJumpTo }: Props) {
    const humanMessages = useMemo(() => {
        return messages
            .map((m, i) => ({ msg: m, index: i }))
            .filter(({ msg }) => msg.role === 'human')
    }, [messages])

    if (humanMessages.length < 2) return null

    return (
        <div className="absolute right-0 top-0 bottom-0 w-6 z-10 pointer-events-none">
            <div className="h-full relative" style={{ padding: '20px 0' }}>
                {humanMessages.map(({ msg, index }) => {
                    const preview = msg.chunks?.map((c) => c.content).join('').slice(0, 20) || '(空)'
                    const top = ((index + 0.5) / messages.length) * 100
                    return (
                        <div
                            key={msg.id}
                            className="absolute right-1 pointer-events-auto group"
                            style={{ top: `${top}%`, transform: 'translateY(-50%)' }}
                        >
                            <button
                                onClick={() => onJumpTo?.(index)}
                                className="block w-1.5 h-1.5 rounded-full bg-gray-300 hover:bg-indigo-500 hover:w-2 hover:h-2 transition-all"
                                title={preview}
                            />
                            {/* Tooltip */}
                            <div className="absolute right-4 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 transition bg-gray-800 text-white text-[10px] px-2 py-1 rounded whitespace-nowrap pointer-events-none">
                                {preview}
                            </div>
                        </div>
                    )
                })}
            </div>
        </div>
    )
}
