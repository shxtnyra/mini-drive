import { Download, Edit3, UserPlus, Folder, Info, Trash2, ChevronRight } from 'lucide-react';
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { useFileStore } from "../store/useFileStore";
import { useFileActions } from '../hooks/useFileActions';

interface ContextMenuProps {
  x: number;
  y: number;
  onClose: () => void;
}

export default function ContextMenu({ x, y, onClose }: ContextMenuProps) {
    const menuRef = useRef<HTMLDivElement>(null);
    const files = useFileStore((state) => state.files);
    const selectedUuids = useFileStore((state) => state.selectedUuids);
    const { deleteFiles, downloadSelected, openRenameModal } = useFileActions();

    const isMultiple = selectedUuids.length > 1;

    const [position, setPosition] = useState({ top: y, left: x });

    useLayoutEffect(() => {
      if (menuRef.current) {
        const { offsetWidth, offsetHeight } = menuRef.current;
        const screenWidth = window.innerWidth;
        const screenHeight = window.innerHeight;

        let correctedX = x;
        let correctedY = y;

        if (x + offsetWidth > screenWidth) {
          correctedX = x - offsetWidth;
        }

        if (y + offsetHeight > screenHeight) {
          correctedY = y - offsetHeight;
        }

        correctedX = Math.max(10, correctedX);
        correctedY = Math.max(10, correctedY);

        setPosition({ top: correctedY, left: correctedX });
      }
    }, [x, y]);

    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
          if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
              onClose();
          }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [onClose]);

    const handleRename = () => {
      if (selectedUuids.length !== 1) return;
      
      const targetItem = files.find((f) => f.uuid === selectedUuids[0]);
      if (targetItem) {
        openRenameModal(targetItem);
      }
      onClose();
    };

    return (
    <div
      ref={menuRef}
      style={{ top: position.top, left: position.left }}
      className="fixed z-50 w-80 bg-page rounded-lg shadow-xl border border-border py-1.5 text-sm font-medium text-primary select-none animate-in fade-in zoom-in-95 duration-100"
    >
      {/* Секция 1: Скачать & Переименовать */}
      <div className="py-1 border-b border-border">
        <button
          onClick={() => { downloadSelected(selectedUuids); onClose(); }}
          className="w-full px-3.5 py-2 flex items-center justify-between hover:bg-sidebar-hover transition-colors cursor-pointer"
        >
          <div className="flex items-center gap-3">
            <Download size={18} className="text-secondary" />
            <span>Скачать</span>
          </div>
        </button>

        <button
          disabled={isMultiple}
          onClick={() => { handleRename(); onClose(); }}
          className="w-full px-3.5 py-2 flex items-center justify-between hover:bg-sidebar-hover transition-colors cursor-pointer disabled:opacity-50 disabled:hover:bg-transparent"
        >
          <div className="flex items-center gap-3">
            <Edit3 size={18} className="text-secondary" />
            <span>Переименовать</span>
          </div>
          <span className="text-xs text-subtle font-mono">Ctrl+Alt+E</span>
        </button>
      </div>

      {/* Секция 2: Поделиться, Упорядочить, Сведения */}
      <div className="py-1 border-b border-border">
        <button
          onClick={() => { console.log('Поделиться'); onClose(); }}
          className="w-full px-3.5 py-2 flex items-center justify-between hover:bg-sidebar-hover transition-colors cursor-pointer"
        >
          <div className="flex items-center gap-3">
            <UserPlus size={18} className="text-secondary" />
            <span>Поделиться</span>
          </div>
          <ChevronRight size={16} className="text-subtle" />
        </button>

        <button
          onClick={() => { console.log('Упорядочить'); onClose(); }}
          className="w-full px-3.5 py-2 flex items-center justify-between hover:bg-sidebar-hover transition-colors cursor-pointer"
        >
          <div className="flex items-center gap-3">
            <Folder size={18} className="text-secondary" />
            <span>Упорядочить</span>
          </div>
          <ChevronRight size={16} className="text-subtle" />
        </button>

        <button
          disabled={isMultiple}
          onClick={() => { console.log('Сведения'); onClose(); }}
          className="w-full px-3.5 py-2 flex items-center justify-between hover:bg-sidebar-hover transition-colors cursor-pointer disabled:opacity-50 disabled:hover:bg-transparent"
        >
          <div className="flex items-center gap-3">
            <Info size={18} className="text-secondary" />
            <span>Сведения о папке</span>
          </div>
          <ChevronRight size={16} className="text-subtle" />
        </button>
      </div>

      {/* Секция 3: Корзина */}
      <div className="py-1">
        <button
          onClick={() => { deleteFiles(selectedUuids); onClose(); }}
          className="w-full px-3.5 py-2 flex items-center justify-between hover:bg-sidebar-hover transition-colors cursor-pointer"
        >
          <div className="flex items-center gap-3">
            <Trash2 size={18} className="text-error-icon" /> {/* Иконка корзины красная для акцента */}
            <span>Отправить в корзину</span>
          </div>
          <span className="text-xs text-subtle font-mono">Delete</span>
        </button>
      </div>
    </div>
  );
}