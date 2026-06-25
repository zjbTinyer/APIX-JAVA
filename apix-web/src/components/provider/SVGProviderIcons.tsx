/** 供应商 SVG 图标映射 */
const PROVIDER_SVGS: Record<string, string> = {
    openai: `<svg viewBox="0 0 24 24" fill="none"><path d="M22.28 10.5a4.5 4.5 0 0 0-2.5-5.82 4.5 4.5 0 0 0-5.82 2.5L12 11.5l-1.96-4.32a4.5 4.5 0 0 0-5.82-2.5 4.5 4.5 0 0 0-2.5 5.82L3.96 15l-1.96 4.32a4.5 4.5 0 0 0 2.5 5.82 4.5 4.5 0 0 0 5.82-2.5L12 18.5l1.96 4.32a4.5 4.5 0 0 0 5.82 2.5 4.5 4.5 0 0 0 2.5-5.82L20.04 15l1.96-4.32Z" fill="currentColor" opacity="0.1"/><path d="M12 2a3 3 0 0 1 3 3v14a3 3 0 1 1-6 0V5a3 3 0 0 1 3-3Z" fill="currentColor" opacity="0.3"/></svg>`,
    deepseek: `<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10" fill="currentColor" opacity="0.1"/><path d="M12 6v12M6 12h12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>`,
    moonshot: `<svg viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z" fill="currentColor"/></svg>`,
    'ollama:local': `<svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="5" fill="currentColor" opacity="0.2"/><ellipse cx="12" cy="19" rx="8" ry="3" fill="currentColor" opacity="0.15"/><circle cx="9" cy="7" r="1" fill="currentColor"/><circle cx="15" cy="7" r="1" fill="currentColor"/></svg>`,
    qwen: `<svg viewBox="0 0 24 24"><path d="M12 2L2 7v10l10 5 10-5V7l-10-5z" fill="currentColor" opacity="0.1"/><path d="M12 6l-6 3v6l6 3 6-3V9l-6-3z" fill="currentColor" opacity="0.3"/></svg>`,
    google: `<svg viewBox="0 0 24 24"><path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="currentColor"/><path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="currentColor" opacity="0.7"/><path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="currentColor" opacity="0.5"/><path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="currentColor" opacity="0.3"/></svg>`,
    claude: `<svg viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z" fill="currentColor"/><circle cx="12" cy="12" r="5" fill="currentColor" opacity="0.3"/></svg>`,
    custom: `<svg viewBox="0 0 24 24"><path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.07.62-.07.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6A3.6 3.6 0 1 1 12 8.4a3.6 3.6 0 0 1 0 7.2z" fill="currentColor"/></svg>`,
}

const PROVIDER_LABELS: Record<string, string> = {
    openai: 'OpenAI', deepseek: 'DeepSeek', moonshot: 'Moonshot',
    'ollama:local': 'Ollama', qwen: '通义千问', google: 'Google',
    claude: 'Claude', custom: '自定义',
}

export function getProviderIcon(name: string): string {
    return PROVIDER_SVGS[name] || PROVIDER_SVGS.custom || ''
}

export function getProviderLabel(name: string): string {
    return PROVIDER_LABELS[name] || name
}

export function ProviderOption({ name, selected }: { name: string; selected?: boolean }) {
    const svg = getProviderIcon(name)
    const label = getProviderLabel(name)
    if (!svg) return <span>{label}</span>
    return (
        <span className="inline-flex items-center gap-1.5">
            <span className="w-4 h-4 inline-block" dangerouslySetInnerHTML={{ __html: svg.replace('currentColor', selected ? '#fff' : '#666') }} />
            <span>{label}</span>
        </span>
    )
}
