import { useState } from 'react';

interface SecurityTabProps {
  onSubmit: (e: React.FormEvent) => void;
  isChanging: boolean;
}

export default function SecurityTab({ onSubmit, isChanging }: SecurityTabProps) {
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  return (
    <form onSubmit={onSubmit} className="space-y-6 max-w-md">
      <div>
        <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">Текущий пароль *</label>
        <input
          type="password"
          value={oldPassword}
          onChange={(e) => setOldPassword(e.target.value)}
          required
          className="w-full bg-transparent border-b border-border text-primary py-3 focus:outline-none focus:border-border-focus transition-colors duration-200 placeholder-placeholder"
          placeholder="••••••••"
        />
      </div>
      <div>
        <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">Новый пароль *</label>
        <input
          type="password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          required
          className="w-full bg-transparent border-b border-border text-primary py-3 focus:outline-none focus:border-border-focus transition-colors duration-200 placeholder-placeholder"
          placeholder="••••••••"
        />
      </div>
      <div>
        <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">Подтвердите новый пароль *</label>
        <input
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          required
          className="w-full bg-transparent border-b border-border text-primary py-3 focus:outline-none focus:border-border-focus transition-colors duration-200 placeholder-placeholder"
          placeholder="••••••••"
        />
      </div>
      
      <div className="pt-4">
        <button
          type="submit"
          disabled={isChanging}
          className="py-2.5 px-6 bg-btn text-btn-text font-medium hover:bg-btn-hover transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center group shadow-btn hover:shadow-btn-hover hover:scale-105 active:scale-95"
        >
          {isChanging ? 'Изменение...' : 'Изменить пароль'}
        </button>
      </div>
    </form>
  );
}