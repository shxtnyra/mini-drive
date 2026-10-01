import { useState, useEffect, useRef } from 'react';
import { X } from 'lucide-react';
import { useUIStore } from '../../store/useUIStore';
import { useFileActions } from '../../hooks/useFileActions';
import Modal from './Modal';

export default function RenameModal() {
  const isOpen = useUIStore((state) => state.isRenameOpen);
  const target = useUIStore((state) => state.renameTarget);
  const closeModal = useUIStore((state) => state.closeRenameModal);
  const { renameFile } = useFileActions();

  const [name, setName] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen && target) {
      setName(target.filename);
      setError(null);
      setTimeout(() => {
        if (inputRef.current) {
          inputRef.current.focus();
          const dotIndex = target.filename.lastIndexOf('.');
          if (target.fileType === 'FILE' && dotIndex > 0) {
            inputRef.current.setSelectionRange(0, dotIndex);
          } else {
            inputRef.current.select();
          }
        }
      }, 50);
    }
  }, [isOpen, target]);

  if (!isOpen || !target) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedName = name.trim();
    if (!trimmedName || isSubmitting) return;
    if (trimmedName === target.filename) {
      closeModal();
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);
      await renameFile(target.uuid, trimmedName);
      closeModal();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Не удалось переименовать');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={closeModal}>
      <div className="flex items-center justify-between mb-8">
        <h2 id="modal-title" className="text-4xl font-light text-primary mb-2 tracking-tight">
            Переименование
        </h2>
        <button
          onClick={closeModal}
          className="text-secondary hover:text-primary transition-colors cursor-pointer"
        >
          <X size={20} />
        </button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">
            Новое название *
          </label>
          <input
            ref={inputRef}
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Введите название..."
            disabled={isSubmitting}
            className="w-full bg-transparent border-b border-border text-primary py-3 focus:outline-none focus:border-border-focus transition-colors duration-200 placeholder-placeholder disabled:opacity-50"
          />
          {error && <p className="text-xs text-error-text mt-2">{error}</p>}
        </div>

        <div className="flex items-center justify-end gap-3 pt-4">
          <button
            type="button"
            onClick={closeModal}
            disabled={isSubmitting}
            className="px-6 py-2.5 text-sm font-medium text-secondary hover:text-primary transition-colors disabled:opacity-50 cursor-pointer"
          >
            Отмена
          </button>
          <button
            type="submit"
            disabled={!name.trim() || isSubmitting}
            className="px-6 py-2.5 text-sm font-semibold bg-btn text-btn-text hover:bg-btn-hover transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
          >
            {isSubmitting ? 'Сохранение...' : 'Готово'}
          </button>
        </div>
      </form>
    </Modal>
  );
}