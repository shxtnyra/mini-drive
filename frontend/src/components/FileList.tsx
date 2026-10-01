import { Loader, AlertCircle, RotateCcw, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { useFileStore } from '../store/useFileStore';
import { useEffect, useRef, useState } from 'react';
import type { NodeResponse } from '../types/api';
import { useNavigate } from 'react-router-dom';
import ContextMenu from './ContextMenu';
import CreateMenu from './CreateMenu';
import { type FileListMode, type ColumnDef } from '../config/fileListConfig';
import { formatBytes } from '../utils/formatters';
import { getFileIcon } from '../utils/fileHelpers';

interface FileListProps {
  mode: FileListMode;
  columns: ColumnDef[];
  onLoadMore?: () => void;
  onRefresh?: () => void;
}

export default function FileList({ mode, columns, onLoadMore, onRefresh }: FileListProps) {
  const files = useFileStore((state) => state.files);
  const viewMode = useFileStore((state) => state.viewMode);
  const hasMore = useFileStore((state) => state.hasMore);
  const isLoading = useFileStore((state) => state.isLoading);
  const isError = useFileStore((state) => state.isError);
  const selectedUuids = useFileStore((state) => state.selectedUuids);
  const sortBy = useFileStore((state) => state.sortBy);
  const sortDirection = useFileStore((state) => state.sortDirection);
  const setSort = useFileStore((state) => state.setSort);

  const selectSingle = useFileStore((state) => state.selectSingle);
  const selectRange = useFileStore((state) => state.selectRange);
  const toggleSelect = useFileStore((state) => state.toggleSelect);
  const clearSelection = useFileStore((state) => state.clearSelection);

  const navigate = useNavigate();
  const bottomRef = useRef<HTMLDivElement>(null);

  const [fileContextMenu, setFileContextMenu] = useState<{ x: number; y: number } | null>(null);
  const [createMenu, setCreateMenu] = useState<{ x: number; y: number } | null>(null);

  const handleContainerClick = () => clearSelection();

  const handleContainerContextMenu = (e: React.MouseEvent) => {
    e.preventDefault();
    if (mode === 'trash' || mode === 'shared' || mode === 'favorite') return;
    
    setFileContextMenu(null);
    setCreateMenu({ x: e.clientX, y: e.clientY });
  };

  const handleSort = (columnKey: string) => {
    let newDirection: 'ASC' | 'DESC' = 'DESC';
    if (sortBy === columnKey) {
      newDirection = sortDirection === 'ASC' ? 'DESC' : 'ASC';
    }
    setSort(columnKey, newDirection);
  };

  const handleFileClick = (e: React.MouseEvent, file: NodeResponse) => {
    e.stopPropagation();
    const isToggleKey = e.ctrlKey || e.metaKey;
    if (isToggleKey) toggleSelect(file.uuid);
    else if (e.shiftKey) selectRange(file.uuid);
    else selectSingle(file.uuid);
  };

  const handleFileDoubleClick = (e: React.MouseEvent, file: NodeResponse) => {
    e.stopPropagation();
    if (file.fileType === 'FOLDER') {
      navigate(`/drive/folders/${file.uuid}`);
    }
  };

  const handleFileContextMenu = (e: React.MouseEvent, file: NodeResponse) => {
    e.preventDefault();
    e.stopPropagation();
    setCreateMenu(null);
    if (!selectedUuids.includes(file.uuid)) selectSingle(file.uuid);
    setFileContextMenu({ x: e.clientX, y: e.clientY });
  };

  useEffect(() => {
    if (!onLoadMore || !hasMore || isLoading || isError) return;
    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) onLoadMore();
    }, { threshold: 0.5 });

    if (bottomRef.current) observer.observe(bottomRef.current);
    return () => observer.disconnect();
  }, [onLoadMore, hasMore, isLoading, isError]);
  
  return (
    <div 
      className="space-y-4 min-h-full pb-20 select-none"
      onClick={handleContainerClick}
      onContextMenu={handleContainerContextMenu}
    >
      {viewMode === 'grid' ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6 gap-4">
          {files.map((file) => (
            <GridItem
              key={file.uuid}
              file={file}
              isSelected={selectedUuids.includes(file.uuid)}
              onClick={(e) => handleFileClick(e, file)}
              onDoubleClick={(e) => handleFileDoubleClick(e, file)}
              onContextMenu={(e) => handleFileContextMenu(e, file)}
            />
          ))}
        </div>
      ) : (
        <div className="bg-surface rounded-lg overflow-hidden border border-border">
          <div className="grid grid-cols-12 gap-4 px-4 py-3 border-b border-border bg-surface text-sm font-medium text-secondary">
            {columns.map((col) => {
              const isSorted = sortBy === col.key;
              return (
                <div 
                  key={col.key} 
                  className={`${col.className} flex items-center gap-1 cursor-pointer hover:bg-sidebar-hover rounded-md px-2 py-1 -mx-2 transition-colors select-none`}
                  onClick={() => handleSort(col.key)}
                  title="Нажмите для сортировки"
                >
                  <span>{col.label}</span>
                  {isSorted ? (
                    sortDirection === 'ASC' ? <ArrowUp size={14} className="text-primary" /> : <ArrowDown size={14} className="text-primary" />
                  ) : (
                    <ArrowUpDown size={14} className="text-subtle" />
                  )}
                </div>
              );  
            })}
          </div>

          {files.map((file) => (
            <ListItem
              key={file.uuid}
              file={file}
              columns={columns}
              isSelected={selectedUuids.includes(file.uuid)}
              onClick={(e) => handleFileClick(e, file)}
              onDoubleClick={(e) => handleFileDoubleClick(e, file)}
              onContextMenu={(e) => handleFileContextMenu(e, file)}
            />
          ))}
        </div>
      )}

      {files.length === 0 && !isLoading && !isError && (
        <div className="flex flex-col items-center justify-center pt-20 text-subtle">
          <p className="text-base">
            {mode === 'trash' ? 'Корзина пуста' : 'Перетащите сюда файлы или нажмите на свободное место'}
          </p>
        </div>
      )}

      {hasMore && (
        <div ref={bottomRef} className="flex justify-center py-6">
          {isLoading && (
            <div className="flex items-center gap-2 text-secondary text-sm">
              <Loader className="animate-spin" size={20} />
              <span>Загрузка файлов...</span>
            </div>
          )}
          {isError && !isLoading && (
            <div className="flex items-center gap-3 bg-error-bg text-error-text px-4 py-2.5 rounded-lg border border-error-border">
              <AlertCircle size={18} className="text-error-icon shrink-0" />
              <span className="text-sm font-medium">Не удалось загрузить файлы</span>
              <button
                onClick={onRefresh}
                className="flex items-center gap-1.5 ml-2 px-3 py-1 bg-btn text-btn-text hover:bg-btn-hover text-xs font-semibold rounded-md transition-colors shadow-sm"
              >
                <RotateCcw size={14} />
                Повторить
              </button>
            </div>
          )}
        </div>
      )}

      {fileContextMenu && (
        <ContextMenu x={fileContextMenu.x} y={fileContextMenu.y} onClose={() => setFileContextMenu(null)} />
      )}
      {createMenu && (
        <CreateMenu x={createMenu.x} y={createMenu.y} onClose={() => setCreateMenu(null)} />
      )}
    </div>
  );
}

function ListItem({
  file,
  isSelected,
  columns,
  onClick,
  onDoubleClick,
  onContextMenu,
}: {
  file: NodeResponse;
  isSelected: boolean;
  columns: ColumnDef[];
  onClick: (e: React.MouseEvent, file: NodeResponse) => void;
  onDoubleClick: (e: React.MouseEvent, file: NodeResponse) => void;
  onContextMenu: (e: React.MouseEvent, file: NodeResponse) => void;
}) {
  return (
    <div
      onClick={(e) => onClick(e, file)}
      onDoubleClick={(e) => onDoubleClick(e, file)}
      onContextMenu={(e) => onContextMenu(e, file)}
      className={`grid grid-cols-12 gap-4 px-4 py-3 border-b border-border last:border-b-0 cursor-pointer
        ${isSelected ? 'bg-selection-bg hover:bg-sidebar-active' : 'hover:bg-sidebar-active'}
      `}
    >
      {columns.map((col) => (
        <div key={col.key} className={`${col.className} flex items-center text-sm text-primary`}>
          {col.render(file)}
        </div>
      ))}
    </div>
  );
}

function GridItem({
  file,
  isSelected,
  onClick,
  onDoubleClick,
  onContextMenu,
}: {
  file: NodeResponse;
  isSelected: boolean;
  onClick: (e: React.MouseEvent, file: NodeResponse) => void;
  onDoubleClick: (e: React.MouseEvent, file: NodeResponse) => void;
  onContextMenu: (e: React.MouseEvent, file: NodeResponse) => void;
}) {
  return (
    <div
      onClick={(e) => onClick(e, file)}
      onDoubleClick={(e) => onDoubleClick(e, file)}
      onContextMenu={(e) => onContextMenu(e, file)}
      className={`rounded-lg p-4 cursor-pointer border transition-all duration-200 group
        ${isSelected 
          ? 'bg-selection-bg border-selection-border shadow-sm' 
          : 'bg-surface border-border hover:bg-sidebar-hover hover:border-selection-border/50'}
      `}
    >
      <div className="aspect-square bg-page rounded-md mb-3 flex items-center justify-center select-none p-4">
        <div className="transform scale-150 text-secondary group-hover:text-primary transition-colors">
          {getFileIcon(file)}
        </div>
      </div>
      
      <p className="text-sm font-medium text-primary truncate" title={file.filename}>
        {file.filename}
      </p>
      
      <p className="text-xs text-secondary mt-1 truncate">
        {file.fileType === 'FOLDER' ? 'Папка' : formatBytes(file.sizeBytes)}
      </p>
    </div>
  );
}