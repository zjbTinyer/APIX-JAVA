import type { Provider } from '../../lib/types'

interface Props {
    provider: Provider
    onToggle: (id: string, enabled: boolean) => void
    onDelete: (id: string) => void
    onEdit: () => void
}

export default function ProviderCard({ provider, onToggle, onDelete, onEdit }: Props) {
    return (
        <div className="bg-white rounded-xl border border-gray-200 p-4 hover:shadow-md transition">
            <div className="flex items-start justify-between mb-3">
                <div>
                    <h3 className="font-medium text-gray-900">{provider.name}</h3>
                    <p className="text-xs text-gray-500 mt-0.5">{provider.endpoint}</p>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                    <input
                        type="checkbox"
                        checked={provider.enabled}
                        onChange={(e) => onToggle(provider.provider_id, e.target.checked)}
                        className="sr-only peer"
                    />
                    <div className="w-9 h-5 bg-gray-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-indigo-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-indigo-500" />
                </label>
            </div>

            {provider.description && (
                <p className="text-xs text-gray-500 mb-3 line-clamp-2">{provider.description}</p>
            )}

            <div className="flex flex-wrap gap-1 mb-3">
                {(provider.model_list || []).slice(0, 5).map((m) => (
                    <span key={m} className="px-2 py-0.5 bg-gray-100 text-gray-600 text-xs rounded-full">{m}</span>
                ))}
                {(provider.model_list?.length || 0) > 5 && (
                    <span className="px-2 py-0.5 bg-gray-100 text-gray-400 text-xs rounded-full">+{provider.model_list!.length - 5}</span>
                )}
            </div>

            <div className="flex items-center justify-between text-xs text-gray-400">
                <span>{provider.type || 'custom'}</span>
                <div className="flex gap-2">
                    <button onClick={onEdit} className="text-indigo-500 hover:text-indigo-700">编辑</button>
                    <button onClick={() => onDelete(provider.provider_id)} className="text-red-400 hover:text-red-600">删除</button>
                </div>
            </div>
        </div>
    )
}
