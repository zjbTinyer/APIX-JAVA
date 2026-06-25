import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import LoginPage from './pages/LoginPage'
import HomeLayout from './pages/HomeLayout'
import AssistPage from './pages/AssistPage'
import DataPage from './pages/DataPage'
import TaskPage from './pages/TaskPage'
import TaskFlowPage from './pages/TaskFlowPage'
import ReportPage from './pages/ReportPage'
import ServerPage from './pages/ServerPage'
import SettingPage from './pages/SettingPage'
import './index.css'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
    const user = useAuthStore((s) => s.user)
    if (!user) return <Navigate to="/login" replace />
    return <>{children}</>
}

function App() {
    const user = useAuthStore((s) => s.user)

    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={user ? <Navigate to="/" replace /> : <LoginPage />} />
                <Route
                    path="/"
                    element={
                        <ProtectedRoute>
                            <HomeLayout />
                        </ProtectedRoute>
                    }
                >
                    <Route index element={<Navigate to="/chat" replace />} />
                    <Route path="chat" element={<AssistPage />} />
                    <Route path="data" element={<DataPage />} />
                    <Route path="tasks" element={<TaskPage />} />
                    <Route path="flow" element={<TaskFlowPage />} />
                    <Route path="reports" element={<ReportPage />} />
                    <Route path="servers" element={<ServerPage />} />
                    <Route path="settings" element={<SettingPage />} />
                </Route>
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    )
}

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <App />
    </React.StrictMode>,
)
