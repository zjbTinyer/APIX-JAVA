import { useState, useEffect } from 'react'
import { getSkills, saveSkills } from '../../lib/api'
import type { Skill } from '../../lib/types'

export default function SkillPage() {
    const [skills, setSkills] = useState<Skill[]>([])
    const [search, setSearch] = useState('')

    useEffect(() => { setSkills(getSkills()) }, [])

    const filtered = skills.filter((s) =>
        [s.name, s.description].some((v) => v?.toLowerCase().includes(search.toLowerCase()))
    )

    const save = (list: Skill[]) => { setSkills(list); saveSkills(list) }

    const handleUpload = () => {
        const input = document.createElement('input')
        input.type = 'file'
        input.accept = '.zip'
        input.onchange = (e) => {
            const file = (e.target as HTMLInputElement).files?.[0]
            if (!file) return
            const skill: Skill = {
                skill_id: `skill_${Date.now()}`,
                name: file.name.replace('.zip', ''),
                description: '',
                version: '1.0.0',
                enabled: true,
                created_at: new Date().toISOString(),
            }
            save([...skills, skill])
        }
        input.click()
    }

    return (
        <div className="p-6 max-w-5xl mx-auto">
            <div className="mb-6">
                <div className="flex items-center justify-between mb-3">
                    <h1 className="text-xl font-bold text-gray-900">Agent 技能包</h1>
                    <button onClick={handleUpload} className="px-4 py-2 bg-indigo-500 hover:bg-indigo-600 text-white text-sm font-medium rounded-lg transition">
                        上传技能包
                    </button>
                </div>
                <input
                    type="text"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    placeholder="通过技能名称、描述搜索..."
                    className="w-full max-w-md px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none focus:ring-2 focus:ring-indigo-500"
                />
            </div>

            <div className="bg-purple-50 border border-purple-200 rounded-lg p-3 mb-6 text-xs text-purple-700 space-y-1">
                <p>1. 技能包是预封装的功能模块，用于执行特定任务。所有代码在本地运行。</p>
                <p>2. 上传 ZIP 格式，根目录下必须包含 SKILL.md 且符合协议要求。</p>
                <p>3. 建议一次只开启与当前任务相关的技能包。</p>
            </div>

            {filtered.length === 0 ? (
                <div className="text-center text-gray-400 py-20">暂无技能包</div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {filtered.map((skill) => (
                        <div key={skill.skill_id} className="bg-white rounded-xl border border-gray-200 p-4 hover:shadow-md transition">
                            <div className="flex items-start justify-between mb-2">
                                <div>
                                    <h3 className="font-medium text-gray-900">{skill.name}</h3>
                                    <p className="text-xs text-gray-400 mt-0.5">v{skill.version}</p>
                                </div>
                                <label className="relative inline-flex items-center cursor-pointer">
                                    <input
                                        type="checkbox"
                                        checked={skill.enabled}
                                        onChange={(e) => save(skills.map((s) => s.skill_id === skill.skill_id ? { ...s, enabled: e.target.checked } : s))}
                                        className="sr-only peer"
                                    />
                                    <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-indigo-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-indigo-500" />
                                </label>
                            </div>
                            {skill.description && <p className="text-xs text-gray-500 mb-2">{skill.description}</p>}
                            <div className="flex items-center justify-between text-xs text-gray-400">
                                <span>{new Date(skill.created_at).toLocaleDateString()}</span>
                                <button onClick={() => save(skills.filter((s) => s.skill_id !== skill.skill_id))} className="text-red-400 hover:text-red-600">删除</button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    )
}
