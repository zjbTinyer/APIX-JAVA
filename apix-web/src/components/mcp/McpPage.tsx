import { useState, useEffect } from 'react'
import { getMcpServices, saveMcpServices } from '../../lib/api'
import type { McpService } from '../../lib/types'

export default function McpPage() {
    const [services, setServices] = useState<McpService[]>([])
    const [search, setSearch] = useState('')
    const [showDialog, setShowDialog] = useState(false)
    const [editing, setEditing] = useState<McpService | null>(null)
    const [form, setForm] = useState({ name: '', endpoint: '', type: 'streamable_http' as McpService['type'], description: '' })

    useEffect(() => { setServices(getMcpServices()) }, [])

    const filtered = services.filter((s) =>
        [s.name, s.endpoint, s.description].some((v) => v?.toLowerCase().includes(search.toLowerCase()))
    )

    const save = (list: McpService[]) => { setServices(list); saveMcpServices(list) }

    const handleSave = () => {
        if (!form.name.trim()) return
        if (editing) {
            save(services.map((s) => s.mcp_id === editing.mcp_id ? { ...s, ...form } : s))
        } else {
            save([...services, {
                mcp_id: `mcp_${Date.now()}`,
                ...form,
                enabled: true,
                created_at: new Date().toISOString(),
            }])
        }
        setShowDialog(false)
        setEditing(null)
        setForm({ name: '', endpoint: '', type: 'streamable_http', description: '' })
    }

    return (
        <div className="p-6 max-w-5xl mx-auto">
            <div className="mb-6">
                <div className="flex items-center justify-between mb-3">
                    <h1 className="text-xl font-bold text-gray-900">MCP 服务</h1>
                    <button
                        onClick={() => { setEditing(null); setForm({ name: '', endpoint: '', type: 'streamable_http', description: '' }); setShowDialog(true) }}
                        className="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 text-white text-sm font-medium rounded-lg transition"
                    >
                        + 新建 MCP
                    </button>
                </div>
                <input
                    type="text"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    placeholder="通过名称、地址、描述搜索..."
                    className="w-full max-w-md px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500"
                />
            </div>

            <div className="bg-cyan-50 border border-cyan-200 rounded-lg p-3 mb-6 text-xs text-cyan-700 space-y-1">
                <p>1. MCP 用于向 Agent 提供工具能力，如文件系统、数据库、浏览器自动化等。</p>
                <p>2. 支持 stdio 与 streamable_http 两种连接方式。系统自动发现工具。</p>
                <p>3. 启用后 Agent 可自动调用其中的工具。建议先测试连接再应用。</p>
            </div>

            {filtered.length === 0 ? (
                <div className="text-center text-gray-400 py-20">暂无 MCP 服务</div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {filtered.map((mcp) => (
                        <div key={mcp.mcp_id} className="bg-white rounded-xl border border-gray-200 p-4 hover:shadow-md transition">
                            <div className="flex items-start justify-between mb-2">
                                <div>
                                    <h3 className="font-medium text-gray-900">{mcp.name}</h3>
                                    <p className="text-xs text-gray-500 mt-0.5 font-mono">{mcp.endpoint}</p>
                                </div>
                                <label className="relative inline-flex items-center cursor-pointer">
                                    <input
                                        type="checkbox"
                                        checked={mcp.enabled}
                                        onChange={(e) => save(services.map((s) => s.mcp_id === mcp.mcp_id ? { ...s, enabled: e.target.checked } : s))}
                                        className="sr-only peer"
                                    />
                                    <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-indigo-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-indigo-500" />
                                </label>
                            </div>
                            {mcp.description && <p className="text-xs text-gray-500 mb-2">{mcp.description}</p>}
                            <div className="flex items-center justify-between text-xs text-gray-400">
                                <span className={`px-2 py-0.5 rounded-full ${mcp.type === 'stdio' ? 'bg-gray-100' : 'bg-blue-100 text-blue-700'}`}>
                                    {mcp.type === 'stdio' ? 'stdio' : 'streamable_http'}
                                </span>
                                <div className="flex gap-2">
                                    <button onClick={() => { setEditing(mcp); setForm({ name: mcp.name, endpoint: mcp.endpoint, type: mcp.type, description: mcp.description }); setShowDialog(true) }} className="text-indigo-500 hover:text-indigo-700">编辑</button>
                                    <button onClick={() => save(services.filter((s) => s.mcp_id !== mcp.mcp_id))} className="text-red-400 hover:text-red-600">删除</button>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {showDialog && (
                <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={() => setShowDialog(false)}>
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md mx-4 p-6" onClick={(e) => e.stopPropagation()}>
                        <h2 className="text-lg font-bold text-gray-900 mb-4">{editing ? '编辑 MCP' : '新建 MCP'}</h2>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm text-gray-600 mb-1">名称 *</label>
                                <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500" />
                            </div>
                            <div>
                                <label className="block text-sm text-gray-600 mb-1">连接地址</label>
                                <input value={form.endpoint} onChange={(e) => setForm({ ...form, endpoint: e.target.value })} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500 font-mono" placeholder="http://localhost:3000/mcp" />
                            </div>
                            <div>
                                <label className="block text-sm text-gray-600 mb-1">连接类型</label>
                                <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value as McpService['type'] })} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500">
                                    <option value="streamable_http">streamable_http</option>
                                    <option value="stdio">stdio</option>
                                </select>
                            </div>
                            <div>
                                <label className="block text-sm text-gray-600 mb-1">描述</label>
                                <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500" rows={2} />
                            </div>
                            <div className="flex justify-end gap-3 pt-2">
                                <button onClick={() => setShowDialog(false)} className="px-4 py-2 text-sm text-gray-600 hover:bg-gray-100 rounded-lg">取消</button>
                                <button onClick={handleSave} className="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 text-white text-sm font-medium rounded-lg">保存</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
