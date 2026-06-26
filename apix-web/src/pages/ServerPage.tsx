import { useState, useEffect, useCallback } from 'react'

interface ServiceInfo {
    name: string
    port: number
    description: string
    icon: React.ReactNode
    status: 'checking' | 'online' | 'offline'
    version?: string
    uptime?: string
    latency?: number
    healthUrl: string
}

const SERVICES: Omit<ServiceInfo, 'status' | 'version' | 'uptime' | 'latency'>[] = [
    {
        name: 'Agent 服务',
        port: 5091,
        description: 'AI Agent 核心引擎，处理对话、工具调用、多 Agent 协作',
        icon: <svg className="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>,
        healthUrl: '/api/v1/health',
    },
    {
        name: '记忆服务',
        port: 5093,
        description: '对话管理、用户认证、短期/长期记忆持久化',
        icon: <svg className="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" /><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" /></svg>,
        healthUrl: '/health',
    },
    {
        name: '文件服务',
        port: 5094,
        description: '文件上传/下载管理，支持多文件操作和 SHA256 校验',
        icon: <svg className="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" /><polyline points="14 2 14 8 20 8" /><line x1="16" y1="13" x2="8" y2="13" /><line x1="16" y1="17" x2="8" y2="17" /></svg>,
        healthUrl: '/file/health',
    },
    {
        name: '任务服务',
        port: 5090,
        description: '任务流编排和执行引擎',
        icon: <svg className="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M9 11l3 3L22 4" /><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" /></svg>,
        healthUrl: '/api/v1/health',
    },
]

function StatusBadge({ status }: { status: ServiceInfo['status'] }) {
    const config = {
        checking: { bg: 'bg-yellow-100 dark:bg-yellow-900/30', dot: 'bg-yellow-400', text: 'text-yellow-700 dark:text-yellow-300', label: '检测中' },
        online: { bg: 'bg-emerald-100 dark:bg-emerald-900/30', dot: 'bg-emerald-500', text: 'text-emerald-700 dark:text-emerald-300', label: '运行中' },
        offline: { bg: 'bg-red-100 dark:bg-red-900/30', dot: 'bg-red-500', text: 'text-red-700 dark:text-red-300', label: '未运行' },
    }[status]
    return (
        <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${config.bg} ${config.text} animate-scale-in`}>
            <span className={`w-1.5 h-1.5 rounded-full ${config.dot} ${status === 'checking' ? 'animate-pulse' : ''}`} />
            {config.label}
        </span>
    )
}

export default function ServerPage() {
    const [services, setServices] = useState<ServiceInfo[]>(
        SERVICES.map((s) => ({ ...s, status: 'checking' as const }))
    )

    const checkService = useCallback(async (svc: Omit<ServiceInfo, 'status' | 'version' | 'uptime' | 'latency'>) => {
        const start = performance.now()
        try {
            const res = await fetch(svc.healthUrl, { signal: AbortSignal.timeout(3000) })
            const latency = Math.round(performance.now() - start)
            const ok = res.ok || res.status < 500
            return {
                ...svc,
                status: ok ? 'online' as const : 'offline' as const,
                latency,
                version: '1.0.0',
            }
        } catch {
            return {
                ...svc,
                status: 'offline' as const,
                latency: undefined,
            }
        }
    }, [])

    useEffect(() => {
        SERVICES.forEach(async (svc) => {
            if (!svc.healthUrl) {
                setServices((prev) => prev.map((s) => s.port === svc.port ? { ...s, status: 'offline', latency: undefined } : s))
                return
            }
            const result = await checkService(svc)
            setServices((prev) => prev.map((s) => s.port === result.port ? { ...s, ...result } : s))
        })
    }, [checkService])

    const handleRefresh = () => {
        setServices(SERVICES.map((s) => ({ ...s, status: 'checking' as const })))
        SERVICES.forEach(async (svc) => {
            if (!svc.healthUrl) {
                setServices((prev) => prev.map((s) => s.port === svc.port ? { ...s, status: 'offline', latency: undefined } : s))
                return
            }
            const result = await checkService(svc)
            setServices((prev) => prev.map((s) => s.port === result.port ? { ...s, ...result } : s))
        })
    }

    const onlineCount = services.filter((s) => s.status === 'online').length

    return (
        <div className="h-full overflow-y-auto">
            <div className="max-w-4xl mx-auto p-6 space-y-6 animate-fade-in">
                {/* 顶栏 */}
                <div className="flex items-center justify-between">
                    <div>
                        <h2 className="text-lg font-bold text-[var(--apix-text)]">服务管理</h2>
                        <p className="text-sm text-[var(--apix-text-secondary)] mt-0.5">查看和管理 APIX 所有微服务运行状态</p>
                    </div>
                    <div className="flex items-center gap-3">
                        <span className="text-xs text-[var(--apix-text-tertiary)]">
                            {onlineCount}/{services.length} 在线
                        </span>
                        <button onClick={handleRefresh}
                            className="px-3 py-1.5 bg-[var(--apix-bg-card)] border border-[var(--apix-border)] text-[var(--apix-text-secondary)] rounded-lg text-xs hover:bg-[var(--apix-primary)] hover:text-white hover:border-[var(--apix-primary)] transition-all duration-200 flex items-center gap-1.5"
                        >
                            <svg className="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="23 4 23 10 17 10" /><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" /></svg>
                            刷新
                        </button>
                    </div>
                </div>

                {/* 统计卡片 */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                    {[
                        { label: '全部服务', value: services.length, color: 'text-[var(--apix-text)]', bg: 'bg-[var(--apix-bg-card)]' },
                        { label: '运行中', value: onlineCount, color: 'text-emerald-500', bg: 'bg-emerald-50 dark:bg-emerald-900/20' },
                        { label: '未运行', value: services.length - onlineCount, color: 'text-red-500', bg: 'bg-red-50 dark:bg-red-900/20' },
                        { label: '检测中', value: services.filter((s) => s.status === 'checking').length, color: 'text-yellow-500', bg: 'bg-yellow-50 dark:bg-yellow-900/20' },
                    ].map((stat) => (
                        <div key={stat.label}
                            className={`${stat.bg} border border-[var(--apix-border)] rounded-xl p-4 transition-theme`}
                        >
                            <div className={`text-2xl font-bold ${stat.color}`}>{stat.value}</div>
                            <div className="text-xs text-[var(--apix-text-secondary)] mt-1">{stat.label}</div>
                        </div>
                    ))}
                </div>

                {/* 服务列表 */}
                <div className="space-y-3">
                    {services.map((svc, index) => (
                        <div key={svc.port}
                            className="bg-[var(--apix-bg-card)] border border-[var(--apix-border)] rounded-xl p-5 transition-theme hover:shadow-md hover:border-[var(--apix-primary)]/20 animate-slide-up"
                            style={{ animationDelay: `${index * 60}ms`, animationFillMode: 'backwards' }}
                        >
                            <div className="flex items-start gap-4">
                                {/* 图标 */}
                                <div className={`w-11 h-11 rounded-xl flex items-center justify-center shrink-0 transition-theme ${svc.status === 'online'
                                    ? 'bg-emerald-100 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400'
                                    : svc.status === 'checking'
                                        ? 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-600 dark:text-yellow-400'
                                        : 'bg-gray-100 dark:bg-gray-800 text-gray-400'
                                    }`}>
                                    {svc.icon}
                                </div>

                                {/* 信息 */}
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-2 mb-1">
                                        <h3 className="font-medium text-[var(--apix-text)]">{svc.name}</h3>
                                        <StatusBadge status={svc.status} />
                                    </div>
                                    <p className="text-xs text-[var(--apix-text-secondary)] mb-2">{svc.description}</p>
                                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-[var(--apix-text-tertiary)]">
                                        <span className="flex items-center gap-1">
                                            <svg className="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" /></svg>
                                            端口 {svc.port}
                                        </span>
                                        {svc.latency !== undefined && (
                                            <span className="flex items-center gap-1">
                                                <svg className="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="22" y1="12" x2="2" y2="12" /><path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z" /></svg>
                                                延迟 {svc.latency}ms
                                            </span>
                                        )}
                                        {svc.version && (
                                            <span className="flex items-center gap-1">
                                                <svg className="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="4 17 10 11 4 5" /><line x1="12" y1="19" x2="20" y2="19" /></svg>
                                                v{svc.version}
                                            </span>
                                        )}
                                    </div>
                                </div>

                                {/* 操作 */}
                                <div className="flex items-center gap-2 shrink-0">
                                    <button
                                        onClick={() => {
                                            const port = svc.port
                                            const healthUrl = SERVICES.find((s) => s.port === port)?.healthUrl
                                            if (healthUrl) {
                                                setServices((prev) => prev.map((s) => s.port === port ? { ...s, status: 'checking' as const } : s))
                                                checkService({ name: svc.name, port, description: svc.description, icon: svc.icon, healthUrl }).then((r) => {
                                                    setServices((prev) => prev.map((s) => s.port === r.port ? { ...s, ...r } : s))
                                                })
                                            }
                                        }}
                                        disabled={svc.status === 'checking'}
                                        className="px-3 py-1.5 text-xs border border-[var(--apix-border)] rounded-lg text-[var(--apix-text-secondary)] hover:bg-[var(--apix-bg)] transition-all duration-200 disabled:opacity-50"
                                    >
                                        检测
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>

                {/* 系统信息 */}
                <div className="bg-[var(--apix-bg-card)] border border-[var(--apix-border)] rounded-xl p-5 transition-theme">
                    <h3 className="font-medium text-[var(--apix-text)] mb-3 text-sm">系统信息</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                        {[
                            { label: '运行平台', value: 'macOS (Darwin)' },
                            { label: 'Java 版本', value: '17+' },
                            { label: 'Spring Boot', value: '3.3.5' },
                            { label: '前端框架', value: 'React 18 + Vite 6' },
                            { label: '数据库', value: 'MySQL 8.0 + Redis 7.0' },
                            { label: '构建工具', value: 'Maven + pnpm' },
                        ].map((item) => (
                            <div key={item.label} className="flex items-center justify-between py-1.5 px-3 bg-[var(--apix-bg)] rounded-lg transition-theme">
                                <span className="text-[var(--apix-text-secondary)]">{item.label}</span>
                                <span className="text-[var(--apix-text)] font-medium">{item.value}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    )
}
