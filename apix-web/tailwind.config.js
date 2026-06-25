/** @type {import('tailwindcss').Config} */
export default {
    content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
    darkMode: 'class',
    theme: {
        extend: {
            colors: {
                apix: {
                    bg: '#f5f5f5',
                    sidebar: '#1e1e2e',
                    primary: '#6366f1',
                    'primary-hover': '#4f46e5',
                    'msg-human': '#6366f1',
                    'msg-ai': '#ffffff',
                    text: '#1e1e2e',
                    'text-secondary': '#6b7280',
                    border: '#e5e7eb',
                },
            },
        },
    },
    plugins: [],
}
