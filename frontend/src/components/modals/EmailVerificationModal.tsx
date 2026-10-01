import { useState } from 'react';
import { X } from 'lucide-react';
import { useUIStore } from '../../store/useUIStore';
import { useAuthStore } from '../../store/useAuthStore';
import { userService } from '../../api/userService';
import Modal from './Modal';

export default function EmailVerificationModal() {
  const { isVerifyEmailOpen, emailAction, closeVerifyEmailModal } = useUIStore();
  const user = useAuthStore((state) => state.user);
  const setTokens = useAuthStore((state) => state.setTokens);
  const loadUser = useAuthStore((state) => state.loadUser);

  const [code, setCode] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isVerifyEmailOpen || !user) return null;

  const isInitial = emailAction === 'initial';
  const title = isInitial ? 'Подтверждение email' : 'Подтверждение нового email';
  const description = isInitial 
    ? `Введите 6-значный код, отправленный на ${user.email}`
    : 'Введите код, отправленный на ваш новый адрес электронной почты';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (code.length !== 6) {
      setError('Код должен состоять из 6 цифр');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      let response;
      if (isInitial) {
        response = await userService.verifyEmail(code);
      } else {
        response = await userService.confirmEmailChange(code);
      }

      setTokens(response.accessToken, response.refreshToken);
      await loadUser();
      closeVerifyEmailModal();
      setCode('');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Неверный код. Попробуйте еще раз.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal isOpen={isVerifyEmailOpen} onClose={closeVerifyEmailModal}>
      {/* Заголовок */}
      <div className="flex items-center justify-between">
        <div>
          <h2 id="modal-title" className="text-4xl font-light text-primary mb-2 tracking-tight">
            {title}
          </h2>
        </div>
        <button
          onClick={closeVerifyEmailModal}
          className="text-secondary hover:text-primary transition-colors cursor-pointer"
        >
          <X size={20} />
        </button>
      </div>
      <p className="text-subtle text-sm mb-4">{description}</p>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">
            Код подтверждения *
          </label>
          <input
            type="text"
            maxLength={6}
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
            placeholder="000000"
            className="w-full bg-transparent border-b border-border text-primary py-3 text-center text-2xl tracking-[0.5em] font-mono focus:outline-none focus:border-border-focus transition-colors duration-200 placeholder-placeholder"
            autoFocus
          />
          {error && <p className="text-xs text-error-text mt-2">{error}</p>}
        </div>

        <div className="flex items-center justify-end gap-3 pt-4">
          <button
            type="button"
            onClick={closeVerifyEmailModal}
            disabled={isLoading}
            className="px-6 py-2.5 text-sm font-medium text-secondary hover:text-primary transition-colors disabled:opacity-50 cursor-pointer"
          >
            Отмена
          </button>
          <button
            type="submit"
            disabled={isLoading || code.length !== 6}
            className="px-6 py-2.5 text-sm font-semibold bg-btn text-btn-text hover:bg-btn-hover transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
          >
            {isLoading ? 'Проверка...' : 'Подтвердить'}
          </button>
        </div>
      </form>
    </Modal>
  );
}