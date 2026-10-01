import { CheckCircle2, AlertCircle, Mail } from 'lucide-react';
import { useUIStore } from '../../store/useUIStore';
import { formatBytes } from '../../utils/formatters';
import type { UserDetail } from '../../types/api';

interface ProfileTabProps {
  user: UserDetail;
  displayName: string;
  setDisplayName: (val: string) => void;
  onSave: () => void;
  isSaving: boolean;
}

export default function ProfileTab({ user, displayName, setDisplayName, onSave, isSaving }: ProfileTabProps) {
  return (
    <div className="space-y-8">
      {/* Отображаемое имя */}
      <div>
        <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">Отображаемое имя</label>
        <div className="flex gap-4">
          <input
            type="text"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            className="flex-1 bg-transparent border-b border-border text-primary py-3 focus:outline-none focus:border-border-focus transition-colors duration-200"
          />
          <button
            onClick={onSave}
            disabled={isSaving}
            className="py-2.5 px-6 bg-btn text-btn-text font-medium hover:bg-btn-hover transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center group shadow-btn hover:shadow-btn-hover hover:scale-105 active:scale-95"
          >
            {isSaving ? 'Сохранение...' : 'Сохранить'}
          </button>
        </div>
      </div>

      {/* Email */}
      <div>
        <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">Электронная почта</label>
        <div className="flex items-center gap-3 py-2">
          <span className="text-primary flex-1">{user.email}</span>
          {user.emailVerified ? (
            <span className="flex items-center gap-1.5 text-xs font-medium text-success-text bg-success-bg px-2.5 py-1 rounded-md">
              <CheckCircle2 size={14} /> Подтвержден
            </span>
          ) : (
            <span className="flex items-center gap-1.5 text-xs font-medium text-error-text bg-error-bg px-2.5 py-1 rounded-md">
              <AlertCircle size={14} /> Не подтвержден
            </span>
          )}
        </div>
        {!user.emailVerified && (
          <button
            onClick={() => useUIStore.getState().openVerifyEmailModal('initial')}
            className="text-sm text-primary hover:text-secondary underline underline-offset-4 transition-colors mt-1"
          >
            Подтвердить email
          </button>
        )}
      </div>

      {/* Смена Email */}
      <div className="pt-6 border-t border-border">
        <button
          onClick={() => useUIStore.getState().openVerifyEmailModal('change')}
          className="flex items-center gap-2 text-sm font-medium text-secondary hover:text-primary transition-colors group"
        >
          <Mail size={16} className="group-hover:text-primary transition-colors" />
          Изменить адрес электронной почты
        </button>
      </div>

      {/* Квота */}
      <div className="pt-6 border-t border-border">
        <p className="text-sm text-secondary">
          Лимит хранилища: <span className="font-medium text-primary">{formatBytes(user.quotaBytes)}</span>
        </p>
      </div>
    </div>
  );
}