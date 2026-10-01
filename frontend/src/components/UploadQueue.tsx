import { useState } from 'react';
import { useUploadStore } from '../store/useTransferStore';
import { 
  X, 
  CheckCircle, 
  AlertCircle, 
  Loader, 
  ChevronDown, 
  ChevronUp, 
  RotateCcw,
  Trash2
} from 'lucide-react';
import { useChunkedUpload } from '../hooks/useChunkedUpload';
import { formatBytes } from '../utils/formatters';
import type { TransferTask } from '../types/ui';

export default function UploadQueue() {
  const uploads = useUploadStore((state) => state.transfers);
  const removeUpload = useUploadStore((state) => state.removeUpload);
  const clearCompleted = useUploadStore((state) => state.clearFinished);
  const { cancelFileUpload, retryFileUpload } = useChunkedUpload();
  const [isMinimized, setIsMinimized] = useState(false);

  if (!uploads || uploads.length === 0) return null;

  const uploadingCount = uploads.filter(
    (u) => u.status === 'uploading' || u.status === 'pending'
  ).length;

  const handleCloseQueue = () => {
    uploads.forEach((upload) => {
      if (upload.status === 'uploading' || upload.status === 'pending') {
        cancelFileUpload(upload.id);
      }
      removeUpload(upload.id);
    });
  };

  return (
    <div className="fixed bottom-6 right-6 w-96 bg-surface rounded-lg shadow-2xl border border-border overflow-hidden z-50 transition-all duration-200">
      {/* Шапка очереди */}
      <div className="bg-surface px-4 py-3 border-b border-border flex items-center justify-between select-none">
        <div className="flex items-center gap-2">
          <h3 className="font-semibold text-sm text-primary">
            Загрузки {uploadingCount > 0 ? `(${uploadingCount})` : ''}
          </h3>
        </div>

        <div className="flex items-center gap-1">
          {/* Очистить завершенные */}
          {uploads.some((u) => u.status === 'completed') && (
            <button
              onClick={clearCompleted}
              className="p-1.5 hover:bg-sidebar-hover rounded transition-colors text-secondary hover:text-primary cursor-pointer"
              title="Очистить завершенные"
            >
              <Trash2 size={16} />
            </button>
          )}

          {/* Кнопка сворачивания */}
          <button
            onClick={() => setIsMinimized(!isMinimized)}
            className="p-1.5 hover:bg-sidebar-hover rounded transition-colors text-secondary hover:text-primary cursor-pointer"
            title={isMinimized ? 'Развернуть' : 'Свернуть'}
          >
            {isMinimized ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
          </button>

          <button
            onClick={handleCloseQueue}
            className="p-1.5 hover:bg-sidebar-hover rounded transition-colors text-secondary hover:text-error-text cursor-pointer"
            title="Закрыть панель и отменить загрузки"
          >
            <X size={18} />
          </button>
        </div>
      </div>

      {/* Список файлов */}
      {!isMinimized && (
        <div className="max-h-96 overflow-y-auto divide-y divide-border">
          {uploads.map((upload) => (
            <UploadItem
              key={upload.id}
              upload={upload}
              onCancel={() => {
                cancelFileUpload(upload.id);
                removeUpload(upload.id);
              }}
              onRetry={() => retryFileUpload(upload.id)}
              onRemove={() => removeUpload(upload.id)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

interface UploadItemProps {
  upload: TransferTask;
  onCancel: () => void;
  onRetry: () => void;
  onRemove: () => void;
}

function UploadItem({ upload, onCancel, onRetry, onRemove }: UploadItemProps) {
  const { file, progress, status, error } = upload;

  const getStatusIcon = () => {
    switch (status) {
      case 'completed':
        return <CheckCircle className="text-success-icon" size={20} />;
      case 'error':
        return <AlertCircle className="text-error-icon" size={20} />;
      case 'uploading':
        return <Loader className="text-primary animate-spin" size={20} />;
      default:
        return <Loader className="text-subtle" size={20} />;
    }
  };

  const getStatusText = () => {
    switch (status) {
      case 'completed':
        return 'Завершено';
      case 'error':
        return error || 'Ошибка загрузки';
      case 'cancelled':
        return 'Отменено';
      case 'uploading':
        return `${Math.round(progress)}%`;
      default:
        return 'Ожидание...';
    }
  };

  return (
    <div className="px-4 py-3 hover:bg-sidebar-hover transition-colors">
      <div className="flex items-start gap-3">
        <div className="mt-0.5">{getStatusIcon()}</div>

        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-primary truncate" title={file?.name}>
            {file?.name}
          </p>
          <p
            className={`text-xs mt-0.5 truncate ${
              status === 'error' ? 'text-error-text' : 'text-secondary'
            }`}
          >
            {formatBytes(file?.size)} • {getStatusText()}
          </p>

          {status === 'uploading' && (
            <div className="mt-2.5">
              <div className="w-full bg-quota-bg rounded-full h-1.5 overflow-hidden">
                <div
                  className="bg-selection-border h-1.5 rounded-full transition-all duration-300"
                  style={{ width: `${Math.min(100, Math.max(0, progress))}%` }}
                />
              </div>
            </div>
          )}
        </div>

        {/* Действия */}
        <div className="flex items-center gap-1">
          {(status === 'error' || status === 'cancelled') && (
            <button
              onClick={onRetry}
              className="p-1.5 hover:bg-surface rounded-full transition-colors text-secondary hover:text-primary cursor-pointer"
              title="Повторить загрузку"
            >
              <RotateCcw size={16} />
            </button>
          )}

          {(status === 'uploading' || status === 'pending') && (
            <button
              onClick={onCancel}
              className="p-1.5 hover:bg-surface rounded-full transition-colors text-secondary hover:text-error-text cursor-pointer"
              title="Отменить"
            >
              <X size={16} />
            </button>
          )}

          {status === 'completed' && (
            <button
              onClick={onRemove}
              className="p-1.5 hover:bg-surface rounded-full transition-colors text-subtle hover:text-primary cursor-pointer"
              title="Убрать из списка"
            >
              <X size={16} />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}