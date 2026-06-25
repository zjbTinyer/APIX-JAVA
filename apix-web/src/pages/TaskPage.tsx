import { useState, useEffect, useRef, useCallback } from 'react'
import { getTasks, saveTasks } from '../lib/api'
import type { AgentTask, TaskStats } from '../lib/types'

function formatDuration(start: string): string {
    const sec = Math.floor((Date.now() - new Date(start).getTime()) / 1000)
    if (sec < 60) return sec + '秒'
    if (sec < 3600) return Math.floor(sec / 60) + '分' + (sec % 60) + '秒'
    return Math.floor(sec / 3600) + '时' + Math.floor((sec % 3600) / 60) + '分'
}

export default function TaskPage() {
    const [tasks, setTasks] = useState<AgentTask[]>([])
    const [search, setSearch] = useState('')
    const [autoRefresh, setAutoRefresh] = useState(false)
    const [confirmMsg, setConfirmMsg] = useState<string | null>(null)
    const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

    const loadTasks = useCallback(() => setTasks(getTasks()), [])
    useEffect(() => { loadTasks() }, [loadTasks])

    useEffect(() => {
        if (autoRefresh) { timerRef.current = setInterval(loadTasks, 3000) }
        else if (timerRef.current) { clearInterval(timerRef.current); timerRef.current = null }
        return () => { if (timerRef.current) clearInterval(timerRef.current) }
    }, [autoRefresh, loadTasks])

    const save = (list: AgentTask[]) => { setTasks(list); saveTasks(list) }

    const clearCompleted = () => { setConfirmMsg('确定要清理所有已完成的任务吗？') }
    const handleConfirm = () => { save(tasks.filter((t) => t.status !== 'completed')); setConfirmMsg(null) }
    const terminateTask = (id: string) => { save(tasks.map((t) => t.task_id === id ? { ...t, status: 'failed' as const, progress: '已终止' } : t)) }

    const filtered = tasks.filter((t) =>
        [t.task_id, t.goal, t.agent_name, (t as any).current_action].some((s) =>
            String(s || '').toLowerCase().includes(search.toLowerCase())
        )
    )

    const stats: TaskStats = {
        total: tasks.length, pending: tasks.filter((t) => t.status === 'pending').length,
        running: tasks.filter((t) => t.status === 'running').length,
        completed: tasks.filter((t) => t.status === 'completed').length,
        failed: tasks.filter((t) => t.status === 'failed').length,
    }

    const statusBadge = (status: string) => {
        const colors: Record<string, string> = { pending: 'bg-yellow-100 text-yellow-700', running: 'bg-blue-100 text-blue-700', completed: 'bg-green-100 text-green-700', failed: 'bg-red-100 text-red-700' }
        const labels: Record<string, string> = { pending: '等待中', running: '运行中', completed: '已完成', failed: '失败' }
        return <span className={'inline-flex px-2 py-0.5 rounded-full text-xs ' + (colors[status] || 'bg-gray-100')}>{labels[status] || status}</span>
    }

    return (
        <div className="h-full overflow-y-auto p-6">
            <div className="max-w-5xl mx-auto">
                <div className="mb-6">
                    <div className="flex items-center justify-between mb-3">
                        <h1 className="text-xl font-bold text-gray-900">后台子代理任务视图</h1>
                        <div className="flex items-center gap-3">
                            <div className="flex items-center gap-2 text-xs text-gray-500">
                                <span>自动刷新</span>
                                <button onClick={() => setAutoRefresh(!autoRefresh)}
                                    className={'px-3 py-1 rounded-lg text-xs font-medium transition ' + (autoRefresh ? 'bg-indigo-100 text-indigo-700' : 'bg-gray-100 text-gray-500')}
                                >{autoRefresh ? 'On' : 'Off'}</button>
                            </div>
                            <button onClick={loadTasks} className="px-3 py-1.5 bg-indigo-500 hover:bg-indigo-600 text-white text-xs font-medium rounded-lg transition">刷新任务</button>
                            <button onClick={clearCompleted} className="px-3 py-1.5 bg-gray-100 hover:bg-gray-200 text-gray-600 text-xs font-medium rounded-lg transition">清理已完成</button>
                        </div>
                    </div>
                    <input type="text" value={search} onChange={(e) => setSearch(e.target.value)}
                        placeholder="通过任务ID、目标、代理名称搜索..."
                        className="w-full max-w-md px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500" />
                </div>

                <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 mb-6 text-xs text-blue-700 space-y-1">
                    <p>1. 子代理是主 Agent 自主分配的后台任务执行者，不干扰用户与主 Agent 的对话。</p>
                    <p>2. 进入设置页开启 Agent 的子代理分配权限后，可在此查看任务状态。</p>
                    <p>3. 子代理任务被中断后通常无法继续执行（蜂群模式除外）。</p>
                </div>

                <div className="flex gap-6 mb-6">
                    {[{ label: '总任务', value: stats.total, color: 'text-gray-900' }, { label: '等待中', value: stats.pending, color: 'text-yellow-600' }, { label: '运行中', value: stats.running, color: 'text-blue-600' }, { label: '已完成', value: stats.completed, color: 'text-green-600' }, { label: '失败', value: stats.failed, color: 'text-red-600' }].map((s) => (
                        <div key={s.label} className="text-center">
                            <div className={'text-2xl font-bold ' + s.color}>{s.value}</div>
                            <div className="text-xs text-gray-500">{s.label}</div>
                        </div>
                    ))}
                </div>

                {filtered.length === 0 ? (
                    <div className="text-center text-gray-400 py-20">暂无任务</div>
                ) : (
                    <div className="space-y-3">
                        {filtered.map((task, i) => (
                            <div key={task.task_id} className="bg-white rounded-xl border border-gray-200 p-4 hover:shadow-md transition" style={{ animationDelay: i * 50 + 'ms' }}>
                                <div className="flex items-start justify-between mb-2">
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 mb-1">
                                            <span className="text-xs text-gray-400 font-mono">{task.task_id.slice(0, 12)}...</span>
                                            {statusBadge(task.status)}
                                            {task.status === 'running' && <span className="text-xs text-gray-400">{formatDuration(task.created_at)}</span>}
                                        </div>
                                        <p className="text-sm font-medium text-gray-900 truncate">{task.goal}</p>
                                        {task.agent_name && <p className="text-xs text-gray-500">代理: {task.agent_name}</p>}
                                    </div>
                                    {(task.status === 'running' || task.status === 'pending') && (
                                        <button onClick={() => terminateTask(task.task_id)}
                                            className="shrink-0 px-2 py-1 text-xs text-red-500 hover:bg-red-50 rounded-lg transition">终止</button>
                                    )}
                                </div>
                                {task.progress && (
                                    <div className="w-full bg-gray-100 rounded-full h-1.5 mt-2">
                                        <div className={'h-1.5 rounded-full ' + (task.status === 'completed' ? 'bg-green-500' : task.status === 'failed' ? 'bg-red-500' : 'bg-indigo-500')}
                                            style={{ width: task.progress + '%' }} />
                                    </div>
                                )}
                                <div className="text-xs text-gray-400 mt-2">{new Date(task.created_at).toLocaleString()}</div>
                            </div>
                        ))}
                    </div>
                )}

                {confirmMsg && (
                    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={() => setConfirmMsg(null)}>
                        <div className="bg-white rounded-xl shadow-xl w-full max-w-sm mx-4 p-6" onClick={(e) => e.stopPropagation()}>
                            <p className="text-sm text-gray-700 mb-4">{confirmMsg}</p>
                            <div className="flex justify-end gap-3">
                                <button onClick={() => setConfirmMsg(null)} className="px-4 py-2 text-sm text-gray-600 hover:bg-gray-100 rounded-lg transition">取消</button>
                                <button onClick={handleConfirm} className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white text-sm font-medium rounded-lg transition">确定</button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}
