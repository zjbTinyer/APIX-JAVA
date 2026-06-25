import { useState, useEffect } from 'react'
import { useChatStore } from '../store/chatStore'
import { fetchModelsList } from '../lib/api'

type ProviderKey = 'openai' | 'deepseek' | 'moonshot' | 'ollama:local' | 'custom'

const PROVIDERS: { key: ProviderKey; label: string }[] = [
    { key: 'openai', label: 'OpenAI' },
    { key: 'deepseek', label: 'DeepSeek' },
    { key: 'moonshot', label: '月之暗面 (Moonshot)' },
    { key: 'ollama:local', label: 'Ollama (本地)' },
    { key: 'custom', label: '自定义供应商' },
]

export default function SettingPage() {
    const { config, setConfig } = useChatStore()
    const [models, setModels] = useState<string[]>([])
    const [loadingModels, setLoadingModels] = useState(false)

    useEffect(() => {
        if (config.modelsProvider && config.apiKey) {
            setLoadingModels(true)
            fetchModelsList(config.modelsProvider, config.apiKey)
                .then((res) => { if (res.code === 200 && Array.isArray(res.data)) setModels(res.data) })
                .catch(console.error)
                .finally(() => setLoadingModels(false))
        }
    }, [config.modelsProvider, config.apiKey])

    const Switch = ({ label, desc, checked, onChange }: { label: string; desc?: string; checked: boolean; onChange: (v: boolean) => void }) => (
        <div className="flex items-center justify-between py-2">
            <div>
                <div className="text-sm text-gray-700">{label}</div>
                {desc && <div className="text-xs text-gray-400">{desc}</div>}
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} className="sr-only peer" />
                <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-indigo-300 rounded-full peer peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-indigo-500" />
            </label>
        </div>
    )

    return (
        <div className="h-full overflow-y-auto">
            <div className="max-w-3xl mx-auto p-6 space-y-8">
                <h2 className="text-lg font-bold text-gray-900">设置</h2>

                {/* APIX 信息 */}
                <section className="bg-gradient-to-r from-indigo-500 to-purple-600 rounded-xl p-6 text-white">
                    <div className="flex items-center gap-3 mb-2">
                        <h1 className="text-2xl font-bold">APIX</h1>
                        <span className="px-2 py-0.5 bg-white/20 rounded text-xs">v1.0.0</span>
                    </div>
                    <p className="text-sm text-white/80 mb-3">
                        一款兼容多引擎的 Agent 平台，支持网页制作、代码编写、文档处理、海报设计等复杂任务
                    </p>
                    <div className="flex gap-2">
                        <a href="https://github.com/JJJJSTIYYYY/Apix" className="px-3 py-1 bg-white/20 rounded-lg text-xs hover:bg-white/30 transition">Github</a>
                        {['Ollama', 'OpenAI', 'DeepSeek', 'MoonShot'].map((t) => (
                            <span key={t} className="px-2 py-1 bg-white/10 rounded text-xs">{t}</span>
                        ))}
                    </div>
                </section>

                {/* 界面设置 */}
                <section className="bg-white rounded-xl border border-gray-200 p-5 space-y-2">
                    <h3 className="font-medium text-gray-900 mb-3 border-b border-gray-100 pb-2">界面设置</h3>
                    <Switch label="在AI消息中显示工具调用标签" desc="开启后实时显示AI当前正在调用的工具名称" checked={config.showToolLabels} onChange={(v) => setConfig({ showToolLabels: v })} />
                    <Switch label="启用深色主题" desc="Beta - 开启后深色主题可能适配不佳" checked={config.darkTheme} onChange={(v) => setConfig({ darkTheme: v })} />
                </section>

                {/* LLM 配置 */}
                <section className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
                    <h3 className="font-medium text-gray-900 border-b border-gray-100 pb-2">LLM 配置</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm text-gray-600 mb-1">供应商</label>
                            <select value={config.modelsProvider} onChange={(e) => setConfig({ modelsProvider: e.target.value, modelName: '' })}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none">
                                <option value="">选择供应商</option>
                                {PROVIDERS.map((p) => <option key={p.key} value={p.key}>{p.label}</option>)}
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm text-gray-600 mb-1">API Key</label>
                            <input type="password" value={config.apiKey} onChange={(e) => setConfig({ apiKey: e.target.value })}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="sk-..." />
                        </div>
                        <div>
                            <label className="block text-sm text-gray-600 mb-1">模型</label>
                            <select value={config.modelName} onChange={(e) => setConfig({ modelName: e.target.value })}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none">
                                <option value="">{loadingModels ? '加载中...' : '选择模型'}</option>
                                {models.map((m) => <option key={m} value={m}>{m}</option>)}
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm text-gray-600 mb-1">模型温度: {config.modelTemperature}</label>
                            <input type="range" min="0" max="100" value={config.modelTemperature * 100}
                                onChange={(e) => setConfig({ modelTemperature: Number(e.target.value) / 100 })}
                                className="w-full accent-indigo-500" />
                        </div>
                    </div>
                    <div className="flex flex-wrap items-center gap-4">
                        <Switch label="深度思考" checked={config.enableThink} onChange={(v) => setConfig({ enableThink: v })} />
                        <Switch label="纯聊天模式" checked={config.pureChatOn} onChange={(v) => setConfig({ pureChatOn: v })} />
                        <Switch label="视觉模型" checked={config.visionOn} onChange={(v) => setConfig({ visionOn: v })} />
                    </div>
                </section>

                {/* 工作区 */}
                <section className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
                    <h3 className="font-medium text-gray-900 border-b border-gray-100 pb-2">工作区</h3>
                    <input type="text" value={config.workDir} onChange={(e) => setConfig({ workDir: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="/path/to/workspace" />
                </section>

                {/* AI 能力 */}
                <section className="bg-white rounded-xl border border-gray-200 p-5 space-y-2">
                    <h3 className="font-medium text-gray-900 border-b border-gray-100 pb-2">AI 能力</h3>
                    {[
                        { key: 'enableFileOperation' as const, label: '允许 Agent 操作文件', desc: 'Agent 可以读取和修改工作区文件' },
                        { key: 'enableWebSearch' as const, label: '允许 Agent 浏览网页', desc: 'Agent 可以联网搜索获取信息' },
                        { key: 'enableKnowledgeRetrieval' as const, label: '允许 Agent 访问知识库', desc: 'Agent 可以从知识库检索文档' },
                        { key: 'enableCommandOperation' as const, label: '允许 Agent 执行命令', desc: 'Agent 可以运行终端命令' },
                        { key: 'enableAgentAssign' as const, label: '启用 Agent 子代理', desc: 'Beta - 主 Agent 可分配子代理执行后台任务' },
                        { key: 'skillLoad' as const, label: '允许 Agent 使用技能包', desc: 'Agent 可加载技能包扩展能力' },
                        { key: 'agentSwarm' as const, label: '启用蜂群模式', desc: 'Beta - 多 Agent 协同工作' },
                        { key: 'enableTaskFlow' as const, label: '启用任务流', desc: '允许 Agent 编排和执行任务流' },
                    ].map((item) => (
                        <Switch key={item.key} label={item.label} desc={item.desc} checked={config[item.key]} onChange={(v) => setConfig({ [item.key]: v })} />
                    ))}
                </section>

                {/* 网络代理 */}
                <section className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
                    <h3 className="font-medium text-gray-900 border-b border-gray-100 pb-2">网络代理</h3>
                    <input type="text" value={config.httpProxyUrl} onChange={(e) => setConfig({ httpProxyUrl: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="HTTP 代理 URL" />
                    <input type="text" value={config.httpsProxyUrl} onChange={(e) => setConfig({ httpsProxyUrl: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="HTTPS 代理 URL" />
                    <input type="text" value={config.excludeUrl} onChange={(e) => setConfig({ excludeUrl: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="排除地址" />
                </section>

                {/* 联网搜索 */}
                <section className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
                    <h3 className="font-medium text-gray-900 border-b border-gray-100 pb-2">联网搜索</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm text-gray-600 mb-1">关键词搜索引擎</label>
                            <input type="text" value={config.linkProvider} onChange={(e) => setConfig({ linkProvider: e.target.value })}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="如: google, bing" />
                        </div>
                        <div>
                            <label className="block text-sm text-gray-600 mb-1">搜索 API Key</label>
                            <input type="password" value={config.linkApiKey} onChange={(e) => setConfig({ linkApiKey: e.target.value })}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="API Key" />
                        </div>
                        <div>
                            <label className="block text-sm text-gray-600 mb-1">内容搜索引擎</label>
                            <input type="text" value={config.contentProvider} onChange={(e) => setConfig({ contentProvider: e.target.value })}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="如: jina, firecrawl" />
                        </div>
                        <div>
                            <label className="block text-sm text-gray-600 mb-1">内容 API Key</label>
                            <input type="password" value={config.contentApiKey} onChange={(e) => setConfig({ contentApiKey: e.target.value })}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="API Key" />
                        </div>
                    </div>
                    <div>
                        <label className="block text-sm text-gray-600 mb-1">内容过滤规则</label>
                        <select value={config.webContentFilter} onChange={(e) => setConfig({ webContentFilter: e.target.value })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none">
                            <option value="llm">LLM 过滤</option>
                            <option value="strict">严格过滤</option>
                            <option value="none">不过滤</option>
                        </select>
                    </div>
                </section>

                {/* 记忆 */}
                <section className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
                    <h3 className="font-medium text-gray-900 border-b border-gray-100 pb-2">记忆</h3>
                    <Switch label="自动整理记忆（长期记忆）" checked={config.enableLongtermMemory} onChange={(v) => setConfig({ enableLongtermMemory: v })} />
                    <Switch label="上下文自动总结（短期记忆）" checked={config.enableShorttermMemory} onChange={(v) => setConfig({ enableShorttermMemory: v })} />
                    <div className="grid grid-cols-2 gap-4 pt-2">
                        <div>
                            <label className="block text-sm text-gray-600 mb-1">总结触发阈值</label>
                            <input type="number" value={config.messageSummary} onChange={(e) => setConfig({ messageSummary: Number(e.target.value) })}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" />
                        </div>
                        <div>
                            <label className="block text-sm text-gray-600 mb-1">保留上下文长度</label>
                            <input type="number" value={config.keepNotSummary} onChange={(e) => setConfig({ keepNotSummary: Number(e.target.value) })}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" />
                        </div>
                    </div>
                    <div>
                        <label className="block text-sm text-gray-600 mb-1">Embedding 模型</label>
                        <input type="text" value={config.embeddingModel} onChange={(e) => setConfig({ embeddingModel: e.target.value })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="如: nomic-embed-text" />
                    </div>

                    <div className="grid grid-cols-2 gap-4 pt-2">
                        <div>
                            <label className="block text-sm text-gray-600 mb-1">LLM 调用预警值 (tokenLimit)</label>
                            <input type="number" value={config.tokenLimit} onChange={(e) => setConfig({ tokenLimit: Number(e.target.value) })}
                                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="0 = 不限" />
                        </div>
                    </div>
                </section>

                {/* 高级 */}
                <section className="bg-white rounded-xl border border-gray-200 p-5 space-y-3">
                    <h3 className="font-medium text-gray-900 border-b border-gray-100 pb-2">高级</h3>
                    <Switch label="保留工具缓存" desc="关闭后每次对话重新加载工具" checked={config.remainToolsCache} onChange={(v) => setConfig({ remainToolsCache: v })} />
                    <Switch label="高级角色提示词权限" desc="允许 AI 使用高级角色提示词" checked={config.higherRolePromptPermission} onChange={(v) => setConfig({ higherRolePromptPermission: v })} />
                    <Switch label="自动同步配置" desc="保存时自动同步配置到后台" checked={config.autoSaveConfig} onChange={(v) => setConfig({ autoSaveConfig: v })} />
                    <div>
                        <label className="block text-sm text-gray-600 mb-1">背景图片 URL</label>
                        <input type="text" value={config.backgroundImage} onChange={(e) => setConfig({ backgroundImage: e.target.value })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none" placeholder="https://..." />
                    </div>
                </section>
            </div>
        </div>
    )
}
