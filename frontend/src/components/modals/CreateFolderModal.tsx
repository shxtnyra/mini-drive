import { useState, useEffect, useRef } from 'react';
import { X } from 'lucide-react';
import { useUIStore } from '../../store/useUIStore';
import { useFileActions } from '../../hooks/useFileActions';
import Modal from './Modal';

export default function CreateFolderModal() {
  const isOpen = useUIStore((state) => state.isCreateFolderOpen);
  const closeModal = useUIStore((state) => state.closeCreateFolder);
  const { createFolder } = useFileActions();

  const [folderName, setFolderName] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isOpen) {
      setFolderName('Новая папка');
      setError(null);
      setTimeout(() => {
        inputRef.current?.focus();
        inputRef.current?.select();
      }, 50);
    }
  }, [isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!folderName.trim() || isSubmitting) return;

    try {
      setIsSubmitting(true);
      setError(null);
      await createFolder(folderName.trim());     
      closeModal();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Не удалось создать папку');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={closeModal}>
      {/* Заголовок */}
      <div className="flex items-center justify-between mb-8">
        <h2 id="modal-title" className="text-4xl font-light text-primary mb-2 tracking-tight">
          Новая папка
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
            Название папки *
          </label>
          <input
            ref={inputRef}
            type="text"
            value={folderName}
            onChange={(e) => setFolderName(e.target.value)}
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
            disabled={!folderName.trim() || isSubmitting}
            className="px-6 py-2.5 text-sm font-semibold bg-btn text-btn-text hover:bg-btn-hover transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
          >
            {isSubmitting ? 'Создание...' : 'Создать'}
          </button>
        </div>
      </form>
    </Modal>
  );
}