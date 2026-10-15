import { 
  Download, FolderOutput, Share2, Trash2, X, RotateCcw,
  File, Image as ImageIcon, Video, FileArchive, 
  AudioLinesIcon
} from "lucide-react";
import { useFileStore } from "../store/useFileStore";
import { useFileActions } from "../hooks/useFileActions";
import { useUIStore } from "../store/useUIStore";
import { useAuthStore } from "../store/useAuthStore";
import FilterDropdown from "./ui/FilterDropdown";
import type { FileFilters } from "../types/ui";
import type { FileListMode } from "../config/fileListConfig";

interface ToolbarAction {
  id: string;
  title: string;
  icon: React.ReactNode;
  disabled?: boolean;
  hoverClass?: string;
  onClick: () => void;
}

interface ToolbarProps {
  mode?: FileListMode;
}

export default function Toolbar({ mode = 'drive' }: ToolbarProps) {
  const selectedUuids = useFileStore((state) => state.selectedUuids);
  const clearSelection = useFileStore((state) => state.clearSelection);
  const filters = useFileStore((state) => state.filters);
  const setFilters = useFileStore((state) => state.setFilters);
  const clearFilters = useFileStore((state) => state.clearFilters);
  const currentUser = useAuthStore((state) => state.user);
  const { deleteFiles, restoreFiles, permanentlyDeleteFiles, downloadSelected } = useFileActions();
  const openMoveFilesModal = useUIStore((state) => state.openMoveFilesModal);
  const openShareModal = useUIStore((state) => state.openShareModal);
  const selectedFile = useFileStore((state) =>
    state.files.find((file) => file.uuid === selectedUuids[0])
  );

  const isSelected = selectedUuids.length > 0;
  const hasActiveFilters = filters.mimeTypeGroup !== 'all' || filters.dateRange !== 'any' || filters.nodeType !== undefined;

  const actions: ToolbarAction[] = mode === 'trash' ? [
    { id: "restore", title: "Восстановить", icon: <RotateCcw size={18} />, onClick: () => restoreFiles(selectedUuids) },
    { id: "permanent-delete", title: "Удалить навсегда", icon: <Trash2 size={18} />, hoverClass: "hover:bg-error-bg hover:text-error-text", onClick: () => {
      if (window.confirm(`Удалить навсегда выбранные элементы (${selectedUuids.length})? Это действие нельзя отменить.`)) {
        void permanentlyDeleteFiles(selectedUuids);
      }
    } },
  ] : [
    { id: "download", title: "Скачать", icon: <Download size={18} />, onClick: () => downloadSelected(selectedUuids) },
    { id: "move", title: "Переместить", icon: <FolderOutput size={18} />, onClick: () => openMoveFilesModal(selectedUuids) },
    { id: "share", title: "Поделиться", icon: <Share2 size={18} />, disabled: selectedUuids.length !== 1 || !selectedFile || selectedFile.ownerUuid !== currentUser?.uuid, onClick: () => selectedFile && openShareModal(selectedFile) },
    { id: "delete", title: "Удалить", icon: <Trash2 size={18} />, hoverClass: "hover:bg-error-bg hover:text-error-text", onClick: () => deleteFiles(selectedUuids) },
  ];

  return (
    <div className="h-10 flex items-center mb-4 select-none">
      {isSelected ? (
        <div className="flex items-center justify-between w-full bg-selection-bg px-3 py-1.5 rounded-lg animate-in fade-in duration-150">
          <div className="flex items-center gap-3">
            <button onClick={clearSelection} className="p-1 hover:bg-sidebar-hover rounded-full transition-colors cursor-pointer text-secondary" title="Снять выделение">
              <X size={18} />
            </button>
            <span className="text-sm font-semibold text-primary">Выделено: {selectedUuids.length}</span>
          </div>
          <div className="flex items-center gap-1">
            {actions.map((action) => (
              <button
                key={action.id}
                onClick={action.onClick}
                disabled={action.disabled}
                className={`p-2 rounded-lg transition-colors cursor-pointer text-secondary disabled:opacity-40 disabled:cursor-not-allowed ${action.hoverClass || "hover:bg-sidebar-hover hover:text-primary"}`}
                title={action.title}
              >
                {action.icon}
              </button>
            ))}
          </div>
        </div>
      ) : mode !== 'trash' ? (
        <div className="flex items-center gap-2 animate-in fade-in duration-150 w-full">
          <FilterDropdown
            label="Тип"
            isActive={filters.mimeTypeGroup !== 'all'}
            selectedValue={filters.mimeTypeGroup || 'all'}
            onSelect={(val) => setFilters({ mimeTypeGroup: val as FileFilters['mimeTypeGroup'], nodeType: val === 'all' ? undefined : 'FILE' })}
            options={[
              { value: 'all', label: 'Все файлы' },
              { value: 'document', label: 'Документы', icon: <File size={14} /> },
              { value: 'image', label: 'Изображения', icon: <ImageIcon size={14} /> },
              { value: 'audio', label: 'Аудио', icon: <AudioLinesIcon size={14} /> },
              { value: 'video', label: 'Видео', icon: <Video size={14} /> },
              { value: 'archive', label: 'Архивы', icon: <FileArchive size={14} /> },
            ]}
          />

          <FilterDropdown
            label="Дата изменения"
            isActive={filters.dateRange !== 'any'}
            selectedValue={filters.dateRange || 'any'}
            onSelect={(val) => setFilters({ dateRange: val as FileFilters['dateRange'] })}
            options={[
              { value: 'any', label: 'Любое время' },
              { value: 'today', label: 'Сегодня' },
              { value: 'week', label: 'На этой неделе' },
              { value: 'month', label: 'В этом месяце' },
              { value: 'year', label: 'В этом году' },
            ]}
          />

          {hasActiveFilters && (
            <button
              onClick={clearFilters}
              className="flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium text-error-text hover:bg-error-bg rounded-lg transition-colors cursor-pointer ml-2"
            >
              <X size={14} />
              <span>Сбросить</span>
            </button>
          )}
        </div>
      ) : null}
    </div>
  );
}