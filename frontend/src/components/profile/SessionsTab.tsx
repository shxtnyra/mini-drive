import { Monitor, Smartphone, LogOut } from 'lucide-react';
import type { Session } from '../../types/api';
import { formatDate } from '../../utils/formatters';

interface SessionsTabProps {
  sessions: Session[];
  onRevokeAll: () => void;
  isRevoking: boolean;
}

export default function SessionsTab({ sessions, onRevokeAll, isRevoking }: SessionsTabProps) {
  return (
    <div className="space-y-4">
      {sessions.length === 0 ? (
        <p className="text-subtle text-center py-8">Нет активных сессий</p>
      ) : (
        sessions.map((session, index) => {
          const isMobile = session.userAgent.toLowerCase().includes('mobile') || 
                           session.deviceName.toLowerCase().includes('android') || 
                           session.deviceName.toLowerCase().includes('ios');
          
          return (
            <div key={session.refreshToken} className="flex items-start gap-4 p-4 border border-border rounded-lg bg-surface">
              {isMobile ? (
                <Smartphone className="text-secondary mt-0.5 shrink-0" size={20} />
              ) : (
                <Monitor className="text-secondary mt-0.5 shrink-0" size={20} />
              )}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-medium text-primary truncate">{session.deviceName || 'Неизвестное устройство'}</span>
                  {index === 0 && (
                    <span className="text-xs font-medium text-primary bg-selection-bg px-2 py-0.5 rounded-md border border-selection-border">
                      Текущее
                    </span>
                  )}
                </div>
                <p className="text-sm text-secondary mt-1 truncate">{session.ipAddress} • {session.userAgent}</p>
                <p className="text-xs text-subtle mt-1">Последняя активность: {formatDate(session.lastUsedAt)}</p>
              </div>
            </div>
          );
        })
      )}
      
      {sessions.length > 1 && (
        <div className="pt-6 border-t border-border">
          <button
            onClick={onRevokeAll}
            disabled={isRevoking}
            className="flex items-center gap-2 text-error-text hover:text-error-icon text-sm font-medium transition-colors disabled:opacity-50"
          >
            <LogOut size={16} />
            {isRevoking ? 'Завершение...' : 'Завершить все остальные сессии'}
          </button>
        </div>
      )}
    </div>
  );
}