import { FolderPlus, FileUp, FolderUp } from 'lucide-react';
import { useEffect, useRef } from 'react';
import { useCreateActions } from '../hooks/useCreateActions.ts';

interface CreateContextMenuProps {
  x: number;
  y: number;
  onClose: () => void;
}

export default function CreateMenu({ x, y, onClose }: CreateContextMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null);
  const { openFilePicker, openFolderPicker, openCreateFolderModal } = useCreateActions();

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        onClose();
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [onClose]);

  return (
    <div
      ref={menuRef}
      style={{ top: y, left: x }}
      className="fixed z-50 w-80 bg-page rounded-lg shadow-xl border border-border py-1.5 text-sm font-medium text-primary select-none animate-in fade-in zoom-in-95 duration-100"
    >
      <div className="border-b border-border pb-1 mb-1">
        <button
          onClick={() => {
            openCreateFolderModal();
            onClose();
          }}
          className="w-full px-4 py-2 flex items-center justify-between hover:bg-sidebar-hover transition-colors cursor-pointer"
        >
          <div className="flex items-center gap-3.5">
            <FolderPlus size={19} className="text-secondary" />
            <span>Новая папка</span>
          </div>
          <span className="text-xs text-subtle font-normal">Alt+C, затем F</span>
        </button>
      </div>

      <div className="space-y-0.5">
        <button
          onClick={() => {
            openFilePicker();
            onClose();
          }}
          className="w-full px-4 py-2 flex items-center justify-between hover:bg-sidebar-hover transition-colors cursor-pointer"
        >
          <div className="flex items-center gap-3.5">
            <FileUp size={19} className="text-secondary" />
            <span>Загрузить файл</span>
          </div>
          <span className="text-xs text-subtle font-normal">Alt+C, затем U</span>
        </button>

        <button
          onClick={() => {
            openFolderPicker();
            onClose();
          }}
          className="w-full px-4 py-2 flex items-center justify-between hover:bg-sidebar-hover transition-colors cursor-pointer"
        >
          <div className="flex items-center gap-3.5">
            <FolderUp size={19} className="text-secondary" />
            <span>Загрузить папку</span>
          </div>
          <span className="text-xs text-subtle font-normal">Alt+C, затем I</span>
        </button>
      </div>
    </div>
  );
} 