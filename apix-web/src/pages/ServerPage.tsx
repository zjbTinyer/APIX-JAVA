export default function ServerPage() {
    return (
        <div className="h-full flex items-center justify-center bg-gray-50">
            <div className="text-center">
                <div className="text-6xl mb-4">🖥️</div>
                <h2 className="text-lg font-bold text-gray-900 mb-2">服务管理</h2>
                <p className="text-sm text-gray-400 mb-4">此页面正在开发中，敬请期待</p>
                <div className="flex flex-col items-center gap-2 text-xs text-gray-400">
                    <span>🔌 Agent 服务 — 端口 5091</span>
                    <span>🗄️ 文件服务 — 端口 5094</span>
                    <span>📋 任务服务 — 端口 5090</span>
                    <span>🧠 记忆服务 — 端口 5093</span>
                </div>
            </div>
        </div>
    )
}
