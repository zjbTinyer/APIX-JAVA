import { useState, useMemo } from 'react'

// Mock data for report page
const MOCK_TASKS = Array.from({ length: 12 }, (_, i) => ({
    id: `task_${i + 1}`,
    goal: `完成数据分析报告第 ${i + 1} 部分`,
    agent_name: `子代理 Alpha-${String.fromCharCode(65 + (i % 5))}`,
    status: (['pending', 'running', 'completed', 'failed'] as const)[i % 4],
    progress: Math.min(100, Math.floor(Math.random() * 120)),
    logs: [
        `[${new Date(Date.now() - (12 - i) * 60000).toLocaleTimeString()}] 任务创建`,
        `[${new Date(Date.now() - (10 - i) * 60000).toLocaleTimeString()}] 开始执行: ${['数据采集', '数据清洗', '特征提取', '模型训练', '结果汇总'][i % 5]}`,
        i % 2 === 0 ? `[${new Date(Date.now() - (5 - i) * 60000).toLocaleTimeString()}] 步骤完成，进度 ${Math.min(100, 30 + i * 10)}%` : '',
        i % 3 === 0 ? `[${new Date(Date.now() - (2 - i) * 60000).toLocaleTimeString()}] 遇到警告: 数据质量偏低` : '',
    ].filter(Boolean),
}))

export default function ReportPage() {
    const [search, setSearch] = useState('')
    const [expanded, setExpanded] = useState<Set<string>>(new Set())

    const filtered = useMemo(() => {
        if (!search.trim()) return MOCK_TASKS
        const q = search.toLowerCase()
        return MOCK_TASKS.filter((t) =>
            [t.id, t.goal, t.agent_name].some((s) => s.toLowerCase().includes(q))
        )
    }, [search])

    const stats = useMemo(() => ({
        total: MOCK_TASKS.length,
        pending: MOCK_TASKS.filter((t) => t.status === 'pending').length,
        running: MOCK_TASKS.filter((t) => t.status === 'running').length,
        completed: MOCK_TASKS.filter((t) => t.status === 'completed').length,
        failed: MOCK_TASKS.filter((t) => t.status === 'failed').length,
    }), [])

    const toggleExpand = (id: string) => {
        setExpanded((prev) => {
            const next = new Set(prev)
            next.has(id) ? next.delete(id) : next.add(id)
            return next
        })
    }

    const statusBadge = (status: string) => {
        const map: Record<string, string> = {
            pending: 'bg-yellow-100 text-yellow-700', running: 'bg-blue-100 text-blue-700',
            completed: 'bg-green-100 text-green-700', failed: 'bg-red-100 text-red-700',
        }
        const label: Record<string, string> = { pending: '等待中', running: '运行中', completed: '已完成', failed: '失败' }
        return <span className={`inline-flex px-2 py-0.5 rounded-full text-xs ${map[status] || 'bg-gray-100'}`}>{label[status] || status}</span>
    }

    return (
        <div className="h-full overflow-y-auto p-6">
            <div className="max-w-5xl mx-auto">
                <div className="mb-6">
                    <h1 className="text-xl font-bold text-gray-900 mb-3">任务流报告</h1>
                    <input
                        type="text"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        placeholder="通过任务ID、目标、代理名称搜索..."
                        className="w-full max-w-md px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500"
                    />
                </div>

                {/* 统计 */}
                <div className="flex gap-6 mb-6">
                    {[
                        { label: '总任务', value: stats.total, color: 'text-gray-900' },
                        { label: '等待中', value: stats.pending, color: 'text-yellow-600' },
                        { label: '运行中', value: stats.running, color: 'text-blue-600' },
                        { label: '已完成', value: stats.completed, color: 'text-green-600' },
                        { label: '失败', value: stats.failed, color: 'text-red-600' },
                    ].map((s) => (
                        <div key={s.label} className="text-center">
                            <div className={`text-2xl font-bold ${s.color}`}>{s.value}</div>
                            <div className="text-xs text-gray-500">{s.label}</div>
                        </div>
                    ))}
                </div>

                {/* 任务列表 */}
                {filtered.length === 0 ? (
                    <div className="text-center text-gray-400 py-20">暂无任务报告</div>
                ) : (
                    <div className="space-y-3">
                        {filtered.map((task) => (
                            <div key={task.id} className="bg-white rounded-xl border border-gray-200 overflow-hidden">
                                <button
                                    onClick={() => toggleExpand(task.id)}
                                    className="w-full text-left px-4 py-3 flex items-center gap-3 hover:bg-gray-50 transition"
                                >
                                    <span className="text-gray-300 text-xs">{expanded.has(task.id) ? '▼' : '▶'}</span>
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 mb-0.5">
                                            <span className="text-xs text-gray-400 font-mono">{task.id}</span>
                                            {statusBadge(task.status)}
                                        </div>
                                        <p className="text-sm font-medium text-gray-900 truncate">{task.goal}</p>
                                        <p className="text-xs text-gray-500">代理: {task.agent_name}</p>
                                    </div>
                                    <div className="w-24">
                                        <div className="w-full bg-gray-100 rounded-full h-1.5">
                                            <div className={`h-1.5 rounded-full ${task.status === 'completed' ? 'bg-green-500' :
                                                task.status === 'failed' ? 'bg-red-500' :
                                                    task.status === 'running' ? 'bg-blue-500' : 'bg-yellow-500'
                                                }`} style={{ width: `${task.progress}%` }} />
                                        </div>
                                        <span className="text-xs text-gray-400">{task.progress}%</span>
                                    </div>
                                </button>

                                {/* 展开日志 */}
                                {expanded.has(task.id) && (
                                    <div className="px-4 pb-3 border-t border-gray-100 pt-2">
                                        <div className="bg-gray-50 rounded-lg p-3 space-y-1">
                                            {task.logs.map((log, i) => (
                                                <p key={i} className={`text-xs font-mono ${log.includes('警告') ? 'text-yellow-600' :
                                                    log.includes('失败') || log.includes('错误') ? 'text-red-600' :
                                                        log.includes('完成') ? 'text-green-600' : 'text-gray-600'
                                                    }`}>{log}</p>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    )
}
