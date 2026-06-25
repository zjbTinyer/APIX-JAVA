import type { ToolCallInfo } from '../lib/types'

interface Props {
    toolCall: ToolCallInfo
    onClose: () => void
}

export default function ToolDetailDialog({ toolCall, onClose }: Props) {
    const formatContent = (data: unknown): string => {
        try { return JSON.stringify(data, null, 2) } catch { return String(data) }
    }

    return (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={onClose}>
            <div className="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 p-5 max-h-[80vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
                <div className="flex items-center justify-between mb-4">
                    <h2 className="text-lg font-bold text-gray-900">工具调用详情</h2>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600">✕</button>
                </div>

                <div className="space-y-3 text-sm">
                    <div className="flex items-center gap-2">
                        <span className="text-gray-500 w-20 shrink-0">工具名称</span>
                        <span className="font-medium text-gray-900">{toolCall.tool_name}</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <span className="text-gray-500 w-20 shrink-0">状态</span>
                        <span className={`px-2 py-0.5 rounded-full text-xs ${toolCall.status === 'completed' ? 'bg-green-100 text-green-700' :
                                toolCall.status === 'in_progress' ? 'bg-blue-100 text-blue-700' :
                                    toolCall.status === 'error' ? 'bg-red-100 text-red-700' :
                                        'bg-yellow-100 text-yellow-700'
                            }`}>{toolCall.status}</span>
                    </div>
                    {toolCall.tool_call_id && (
                        <div className="flex items-center gap-2">
                            <span className="text-gray-500 w-20 shrink-0">调用 ID</span>
                            <span className="text-gray-600 font-mono text-xs break-all">{toolCall.tool_call_id}</span>
                        </div>
                    )}

                    <div>
                        <span className="text-gray-500 block mb-1">返回数据</span>
                        <pre className="bg-gray-50 border border-gray-200 rounded-lg p-3 text-xs font-mono text-gray-700 overflow-x-auto max-h-60">
                            {formatContent(toolCall.content)}
                        </pre>
                    </div>
                </div>
            </div>
        </div>
    )
}
