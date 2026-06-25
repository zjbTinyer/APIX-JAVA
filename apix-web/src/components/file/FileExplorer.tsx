import { useState } from 'react'
import type { FileNode } from '../../lib/types'

interface Props {
    onOpenFile?: (path: string) => void
}

const SAMPLE_TREE: FileNode = {
    name: 'workspace',
    path: '/workspace',
    type: 'directory',
    children: [
        { name: 'README.md', path: '/workspace/README.md', type: 'file' },
        { name: 'src', path: '/workspace/src', type: 'directory', children: [
            { name: 'main.py', path: '/workspace/src/main.py', type: 'file' },
            { name: 'utils', path: '/workspace/src/utils', type: 'directory', children: [
                { name: 'helpers.py', path: '/workspace/src/utils/helpers.py', type: 'file' },
            ]},
        ]},
        { name: 'data', path: '/workspace/data', type: 'directory', children: [] },
    ],
}

export default function FileExplorer({ onOpenFile }: Props) {
    const [tree] = useState<FileNode>(SAMPLE_TREE)
    const [expanded, setExpanded] = useState<Set<string>>(new Set(['/workspace']))
    const [showNewInput, setShowNewInput] = useState(false)
    const [newName, setNewName] = useState('')

    const toggleExpand = (path: string) => {
        setExpanded((prev) => {
            const next = new Set(prev)
            next.has(path) ? next.delete(path) : next.add(path)
            return next
        })
    }

    const renderNode = (node: FileNode, depth: number) => {
        const isExpanded = expanded.has(node.path)
        const isDir = node.type === 'directory'
        return (
            <div key={node.path}>
                <div className="flex items-center gap-1 px-2 py-1 rounded hover:bg-gray-100 cursor-pointer text-sm group"
                    style={{ paddingLeft: `${depth * 16 + 8}px` }}
                    onClick={() => isDir ? toggleExpand(node.path) : onOpenFile?.(node.path)}
                >
                    {isDir && <span className="text-xs text-gray-400 w-4 shrink-0">{isExpanded ? '▼' : '▶'}</span>}
                    {!isDir && <span className="w-4 shrink-0" />}
                    <span className="text-xs shrink-0">{isDir ? (isExpanded ? '📂' : '📁') : '📄'}</span>
                    <span className="truncate text-gray-700">{node.name}</span>
                </div>
                {isDir && isExpanded && node.children?.map((child) => renderNode(child, depth + 1))}
            </div>
        )
    }

    return (
        <div className="h-full flex flex-col bg-white border-r border-gray-200 shrink-0">
            <div className="flex items-center justify-between px-3 py-2 border-b border-gray-100 shrink-0">
                <span className="text-sm font-medium text-gray-700">文件</span>
                <button onClick={() => setShowNewInput(!showNewInput)}
                    className="p-1 hover:bg-gray-100 rounded text-gray-400 hover:text-gray-600 text-xs">+</button>
            </div>
            {showNewInput && (
                <div className="px-3 py-2 border-b border-gray-100">
                    <input value={newName} onChange={(e) => setNewName(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key === 'Enter' && newName.trim()) { setNewName(''); setShowNewInput(false) }
                            if (e.key === 'Escape') { setNewName(''); setShowNewInput(false) }
                        }}
                        placeholder="文件名" className="w-full px-2 py-1 border border-gray-300 rounded text-xs outline-none focus:ring-1 focus:ring-indigo-500" autoFocus />
                </div>
            )}
            <div className="flex-1 overflow-y-auto py-1">
                {renderNode(tree, 0)}
            </div>
        </div>
    )
}
