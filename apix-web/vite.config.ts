import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        proxy: {
            '/api': { target: 'http://localhost:5091', changeOrigin: true },
            '/ws': { target: 'ws://localhost:5091', ws: true },
            '/auth': { target: 'http://localhost:5093', changeOrigin: true },
            '/memory': { target: 'http://localhost:5093', changeOrigin: true },
            '/user': { target: 'http://localhost:5093', changeOrigin: true },
            '/file': { target: 'http://localhost:5094', changeOrigin: true },
        },
    },
})
