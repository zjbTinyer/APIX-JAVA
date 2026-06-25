import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import * as api from '../lib/api'

const APP_VERSION = '1.0.0'

export default function LoginPage() {
    const [tab, setTab] = useState<'login' | 'register'>('login')
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')
    const [toast, setToast] = useState('')
    const setUser = useAuthStore((s) => s.setUser)
    const navigate = useNavigate()

    useEffect(() => { setConfirmPassword(''); setError('') }, [tab])

    const showToast = (msg: string) => { setToast(msg); setTimeout(() => setToast(''), 3000) }

    const handleSubmit = async () => {
        if (!username || !password) { setError('请输入用户名和密码'); return }
        if (tab === 'register' && password !== confirmPassword) { setError('两次密码不一致'); return }
        setLoading(true)
        setError('')
        try {
            const fn = tab === 'login' ? api.login : api.register
            const res = await fn(username, password)
            if (res.success) {
                if (tab === 'register') showToast('注册成功！')
                setUser({ username, userUid: res.messages.uid, token: res.messages.token })
                setTimeout(() => navigate('/', { replace: true }), tab === 'register' ? 300 : 0)
            } else {
                setError(res.messages || '操作失败')
            }
        } catch (e: unknown) {
            setError(String(e))
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-50 to-purple-50">
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-8 mx-4">
                {/* Logo */}
                <div className="flex flex-col items-center mb-8">
                    <div className="w-16 h-16 bg-indigo-500 rounded-2xl flex items-center justify-center text-white text-2xl font-bold mb-3">
                        A
                    </div>
                    <h1 className="text-2xl font-bold text-gray-900">APIX</h1>
                    <p className="text-gray-500 text-sm mt-1">AI Agent 协作平台</p>
                </div>

                {/* Tabs */}
                <div className="flex mb-6 bg-gray-100 rounded-lg p-1">
                    <button
                        className={`flex-1 py-2 text-sm font-medium rounded-md transition ${tab === 'login' ? 'bg-white shadow-sm text-indigo-600' : 'text-gray-500'}`}
                        onClick={() => setTab('login')}
                    >
                        登录
                    </button>
                    <button
                        className={`flex-1 py-2 text-sm font-medium rounded-md transition ${tab === 'register' ? 'bg-white shadow-sm text-indigo-600' : 'text-gray-500'}`}
                        onClick={() => setTab('register')}
                    >
                        注册
                    </button>
                </div>

                {/* Form */}
                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">用户名</label>
                        <input
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
                            placeholder="请输入用户名"
                            onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">密码</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition"
                            placeholder="请输入密码"
                            onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
                        />
                    </div>

                    {tab === 'register' && (
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">确认密码</label>
                            <input
                                type="password"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                className={`w-full px-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none transition ${confirmPassword && password !== confirmPassword ? 'border-red-300 bg-red-50' : 'border-gray-300'
                                    }`}
                                placeholder="再次输入密码"
                                onKeyDown={(e) => e.key === 'Enter' && handleSubmit()}
                            />
                            {confirmPassword && password !== confirmPassword && (
                                <p className="text-xs text-red-500 mt-1">密码不一致</p>
                            )}
                        </div>
                    )}

                    {error && <div className="text-red-500 text-sm bg-red-50 px-3 py-2 rounded-lg">{error}</div>}

                    <button
                        onClick={handleSubmit}
                        disabled={loading}
                        className="w-full py-2.5 bg-indigo-500 hover:bg-indigo-600 text-white font-medium rounded-lg transition disabled:opacity-50"
                    >
                        {loading ? '处理中...' : tab === 'login' ? '登录' : '注册'}
                    </button>

                    {tab === 'login' && (
                        <button className="w-full text-center text-xs text-gray-400 hover:text-gray-600 transition">
                            忘记密码？
                        </button>
                    )}
                </div>

                <div className="text-center text-xs text-gray-400 mt-6">
                    APIX v{APP_VERSION}
                </div>
            </div>

            {/* Toast */}
            {toast && (
                <div className="fixed top-4 left-1/2 -translate-x-1/2 bg-gray-800 text-white px-4 py-2 rounded-lg text-sm shadow-lg z-50">
                    {toast}
                </div>
            )}
        </div>
    )
}
