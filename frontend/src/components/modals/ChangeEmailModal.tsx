import { useState } from 'react';
import { X } from 'lucide-react';
import { useUIStore } from '../../store/useUIStore';
import { userService } from '../../api/userService';
import Modal from './Modal';

interface ChangeEmailModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function ChangeEmailModal({ isOpen, onClose }: ChangeEmailModalProps) {
  const [newEmail, setNewEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    try {
      await userService.requestEmailChange(newEmail, password);
      onClose();
      setNewEmail('');
      setPassword('');
      useUIStore.getState().openVerifyEmailModal('change');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка при запросе смены email');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <div className="flex items-start justify-between mb-8">
        <div>
          <h2 className="text-4xl font-light text-primary mb-2 tracking-tight">Смена email</h2>
          <p className="text-subtle text-sm">Введите новый адрес и текущий пароль для подтверждения</p>
        </div>
        <button onClick={onClose} className="text-secondary hover:text-primary transition-colors -mt-2 -mr-2 p-2">
          <X size={20} />
        </button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">Новый email *</label>
          <input
            type="email"
            value={newEmail}
            onChange={(e) => setNewEmail(e.target.value)}
            required
            className="w-full bg-transparent border-b border-border text-primary py-3 focus:outline-none focus:border-border-focus transition-colors duration-200 placeholder-placeholder"
            placeholder="new@example.com"
          />
        </div>
        <div>
          <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">Текущий пароль *</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="w-full bg-transparent border-b border-border text-primary py-3 focus:outline-none focus:border-border-focus transition-colors duration-200 placeholder-placeholder"
            placeholder="••••••••"
          />
          {error && <p className="text-xs text-error-text mt-2">{error}</p>}
        </div>

        <div className="flex items-center justify-end gap-3 pt-4">
          <button type="button" onClick={onClose} className="px-6 py-2.5 text-sm font-medium text-secondary hover:text-primary transition-colors">
            Отмена
          </button>
          <button
            type="submit"
            disabled={isLoading}
            className="px-6 py-2.5 text-sm font-semibold bg-btn text-btn-text hover:bg-btn-hover transition-all duration-300 disabled:opacity-50"
          >
            {isLoading ? 'Отправка...' : 'Отправить код'}
          </button>
        </div>
      </form>
    </Modal>
  );
}