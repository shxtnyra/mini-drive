import { useEffect, useState } from 'react';
import { ArrowLeft, Folder, X } from 'lucide-react';
import { fileService } from '../../api/fileService';
import { useFileActions } from '../../hooks/useFileActions';
import { useUIStore } from '../../store/useUIStore';
import type { BreadcrumbItem, NodeResponse } from '../../types/api';
import { getApiErrorMessage } from '../../utils/apiErrors';
import Modal from './Modal';

export default function MoveItemsModal() {
  const isOpen = useUIStore((state) => state.isMoveFilesOpen);
  const movingUuids = useUIStore((state) => state.moveFileUuids);
  const closeModal = useUIStore((state) => state.closeMoveFilesModal);
  const { moveFiles } = useFileActions();

  const [folderUuid, setFolderUuid] = useState<string | null>(null);
  const [targetUuid, setTargetUuid] = useState<string | null>(null);
  const [folders, setFolders] = useState<NodeResponse[]>([]);
  const [breadcrumbs, setBreadcrumbs] = useState<BreadcrumbItem[]>([]);
  const [loadedFolderUuid, setLoadedFolderUuid] = useState<string | null | undefined>(undefined);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const isLoading = loadedFolderUuid !== folderUuid;

  useEffect(() => {
    if (!isOpen) return;

    let isCurrent = true;
    fileService.getFiles({
      context: 'DRIVE',
      parentUuid: folderUuid ?? undefined,
      nodeType: 'FOLDER',
      size: 100,
    }).then((response) => {
      if (!isCurrent) return;
      setFolders(response.files.items);
      setBreadcrumbs(response.breadcrumbs);
      setLoadedFolderUuid(folderUuid);
    }).catch((requestError: unknown) => {
      if (isCurrent) {
        setError(getApiErrorMessage(requestError, 'Не удалось загрузить папки'));
        setLoadedFolderUuid(folderUuid);
      }
    });

    return () => {
      isCurrent = false;
    };
  }, [isOpen, folderUuid]);

  const handleMove = async () => {
    if (isSubmitting) return;
    try {
      setIsSubmitting(true);
      setError(null);
      await moveFiles(movingUuids, targetUuid);
      closeModal();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Не удалось переместить элементы'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={closeModal}>
      <div className="flex items-center justify-between mb-6">
        <h2 id="modal-title" className="text-3xl font-light text-primary tracking-tight">
          Перемещение
        </h2>
        <button
          onClick={closeModal}
          className="text-secondary hover:text-primary transition-colors cursor-pointer"
          aria-label="Закрыть"
        >
          <X size={20} />
        </button>
      </div>

      <p className="text-sm text-secondary mb-4">
        Выбрано элементов: {movingUuids.length}. Укажите папку назначения.
      </p>

      <div className="flex flex-wrap items-center gap-1 mb-3 text-sm">
        <button
          onClick={() => {
            setError(null);
            setFolderUuid(null);
            setTargetUuid(null);
          }}
          className="text-secondary hover:text-primary cursor-pointer"
        >
          Мой диск
        </button>
        {breadcrumbs.filter((crumb) => crumb.uuid !== null).map((crumb) => (
          <span key={crumb.uuid} className="flex items-center gap-1">
            <span className="text-subtle">/</span>
            <button
              onClick={() => {
                setError(null);
                setFolderUuid(crumb.uuid);
                setTargetUuid(crumb.uuid);
              }}
              className="text-secondary hover:text-primary cursor-pointer"
            >
              {crumb.name}
            </button>
          </span>
        ))}
      </div>

      <div className="max-h-64 overflow-y-auto rounded-lg border border-border divide-y divide-border">
        {folderUuid && (
          <button
            onClick={() => {
              const parent = [...breadcrumbs].reverse().find((crumb) => crumb.uuid !== folderUuid);
              setError(null);
              setFolderUuid(parent?.uuid ?? null);
              setTargetUuid(parent?.uuid ?? null);
            }}
            className="w-full flex items-center gap-3 px-3 py-3 text-sm text-secondary hover:bg-sidebar-hover cursor-pointer"
          >
            <ArrowLeft size={17} />
            Назад
          </button>
        )}

        {isLoading ? (
          <p className="px-3 py-6 text-center text-sm text-secondary">Загрузка папок...</p>
        ) : folders.length === 0 ? (
          <p className="px-3 py-6 text-center text-sm text-secondary">В этой папке нет вложенных папок</p>
        ) : folders.map((folder) => {
          const cannotMoveIntoItself = movingUuids.includes(folder.uuid);
          return (
            <div key={folder.uuid} className="flex items-center gap-2 px-3 py-2.5 hover:bg-sidebar-hover">
              <button
                disabled={cannotMoveIntoItself}
                onClick={() => {
                  setError(null);
                  setFolderUuid(folder.uuid);
                  setTargetUuid(folder.uuid);
                }}
                className="flex flex-1 min-w-0 items-center gap-3 text-left text-sm text-primary disabled:opacity-40 cursor-pointer"
              >
                <Folder size={18} className="shrink-0 text-secondary" />
                <span className="truncate">{folder.filename}</span>
              </button>
              <button
                disabled={cannotMoveIntoItself}
                onClick={() => setTargetUuid(folder.uuid)}
                className={`px-2.5 py-1 text-xs rounded-md cursor-pointer ${
                  targetUuid === folder.uuid
                    ? 'bg-selection-bg text-primary'
                    : 'text-secondary hover:bg-surface'
                } disabled:opacity-40`}
              >
                {targetUuid === folder.uuid ? 'Выбрана' : 'Выбрать'}
              </button>
            </div>
          );
        })}
      </div>

      <button
        onClick={() => setTargetUuid(null)}
        className={`w-full mt-3 px-3 py-2.5 rounded-lg border text-left text-sm transition-colors cursor-pointer ${
          targetUuid === null
            ? 'border-selection-border bg-selection-bg text-primary'
            : 'border-border text-secondary hover:bg-sidebar-hover'
        }`}
      >
        Мой диск (корень)
      </button>

      {error && <p className="text-xs text-error-text mt-3">{error}</p>}

      <div className="flex items-center justify-end gap-3 pt-5">
        <button
          type="button"
          onClick={closeModal}
          disabled={isSubmitting}
          className="px-5 py-2.5 text-sm font-medium text-secondary hover:text-primary transition-colors disabled:opacity-50 cursor-pointer"
        >
          Отмена
        </button>
        <button
          type="button"
          onClick={handleMove}
          disabled={isSubmitting || movingUuids.length === 0}
          className="px-5 py-2.5 text-sm font-semibold bg-btn text-btn-text hover:bg-btn-hover transition-colors disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
        >
          {isSubmitting ? 'Перемещение...' : 'Переместить'}
        </button>
      </div>
    </Modal>
  );
}
