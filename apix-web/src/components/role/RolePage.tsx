import { useState, useEffect } from 'react'
import { getRoles, saveRoles } from '../../lib/api'
import type { Role } from '../../lib/types'

export default function RolePage() {
    const [roles, setRoles] = useState<Role[]>([])
    const [showDialog, setShowDialog] = useState(false)
    const [editing, setEditing] = useState<Role | null>(null)
    const [form, setForm] = useState({ name: '', definition: '' })

    useEffect(() => { setRoles(getRoles()) }, [])

    const save = (list: Role[]) => { setRoles(list); saveRoles(list) }

    const handleSave = () => {
        if (!form.name.trim()) return
        if (editing) {
            save(roles.map((r) => r.role_id === editing.role_id ? { ...r, ...form } : r))
        } else {
            save([...roles, {
                role_id: `role_${Date.now()}`,
                name: form.name,
                definition: form.definition,
                enabled: true,
                created_at: new Date().toISOString(),
            }])
        }
        setShowDialog(false)
        setEditing(null)
        setForm({ name: '', definition: '' })
    }

    const openEdit = (role: Role) => {
        setEditing(role)
        setForm({ name: role.name, definition: role.definition })
        setShowDialog(true)
    }

    return (
        <div className="p-6 max-w-5xl mx-auto">
            <div className="mb-6">
                <div className="flex items-center justify-between mb-3">
                    <h1 className="text-xl font-bold text-gray-900">角色卡</h1>
                    <button
                        onClick={() => { setEditing(null); setForm({ name: '', definition: '' }); setShowDialog(true) }}
                        className="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 text-white text-sm font-medium rounded-lg transition"
                    >
                        + 新建角色
                    </button>
                </div>
            </div>

            {roles.length === 0 ? (
                <div className="text-center text-gray-400 py-20">暂无角色卡</div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {roles.map((role) => (
                        <div key={role.role_id} className="bg-white rounded-xl border border-gray-200 p-4 hover:shadow-md transition">
                            <div className="flex items-start justify-between mb-2">
                                <h3 className="font-medium text-gray-900">{role.name}</h3>
                                <label className="relative inline-flex items-center cursor-pointer">
                                    <input
                                        type="checkbox"
                                        checked={role.enabled}
                                        onChange={(e) => save(roles.map((r) => r.role_id === role.role_id ? { ...r, enabled: e.target.checked } : r))}
                                        className="sr-only peer"
                                    />
                                    <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-indigo-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-indigo-500" />
                                </label>
                            </div>
                            <p className="text-xs text-gray-500 mb-3 line-clamp-3">{role.definition}</p>
                            <div className="flex items-center justify-between text-xs text-gray-400">
                                <span>{new Date(role.created_at).toLocaleDateString()}</span>
                                <div className="flex gap-2">
                                    <button onClick={() => openEdit(role)} className="text-indigo-500 hover:text-indigo-700">编辑</button>
                                    <button onClick={() => save(roles.filter((r) => r.role_id !== role.role_id))} className="text-red-400 hover:text-red-600">删除</button>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Dialog */}
            {showDialog && (
                <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={() => setShowDialog(false)}>
                    <div className="bg-white rounded-xl shadow-xl w-full max-w-md mx-4 p-6" onClick={(e) => e.stopPropagation()}>
                        <h2 className="text-lg font-bold text-gray-900 mb-4">{editing ? '编辑角色' : '新建角色'}</h2>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm text-gray-600 mb-1">名称 *</label>
                                <input
                                    value={form.name}
                                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500"
                                    placeholder="角色名称"
                                />
                            </div>
                            <div>
                                <label className="block text-sm text-gray-600 mb-1">定义 / 提示词</label>
                                <textarea
                                    value={form.definition}
                                    onChange={(e) => setForm({ ...form, definition: e.target.value })}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500 font-mono"
                                    rows={6}
                                    placeholder="你是一个...扮演..."
                                />
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
