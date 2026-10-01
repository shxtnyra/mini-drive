import { useState, useEffect } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { userService } from '../api/userService';
import { User, Shield, Monitor } from 'lucide-react';
import ProfileTab from '../components/profile/ProfileTab';
import SecurityTab from '../components/profile/SecurityTab';
import SessionsTab from '../components/profile/SessionsTab';
import type { Session } from '../types/api';
import ChangeEmailModal from '../components/modals/ChangeEmailModal';

type Tab = 'profile' | 'security' | 'sessions';

export default function ProfilePage() {
  const [activeTab, setActiveTab] = useState<Tab>('profile');
  const user = useAuthStore((state) => state.user);
  const setUser = useAuthStore((state) => state.setUser);
  const logout = useAuthStore((state) => state.logout);

  const [displayName, setDisplayName] = useState(user?.displayName || '');
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [isRevoking, setIsRevoking] = useState(false);
  const [isChangeEmailOpen, setIsChangeEmailOpen] = useState(false);

  useEffect(() => {
    if (user) setDisplayName(user.displayName || '');
  }, [user]);

  useEffect(() => {
    if (activeTab === 'sessions') loadSessions();
  }, [activeTab]);

  const loadSessions = async () => {
    try {
      const data = await userService.getSessions();
      setSessions(data);
    } catch (err) {
      console.error('Failed to load sessions', err);
    }
  };

  const handleSaveProfile = async () => {
    setIsSavingProfile(true);
    try {
      const updatedUser = await userService.updateProfile(displayName);
      setUser(updatedUser);
      alert('Профиль успешно обновлен');
    } catch (err) {
      alert('Ошибка при сохранении профиля');
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    const form = e.target as HTMLFormElement;
    const formData = new FormData(form);
    const oldPass = formData.get('oldPassword') as string;
    const newPass = formData.get('newPassword') as string;
    const confirmPass = formData.get('confirmPassword') as string;

    if (newPass !== confirmPass) {
      alert('Новые пароли не совпадают');
      return;
    }
    
    setIsChangingPassword(true);
    try {
      await userService.changePassword(oldPass, newPass);
      alert('Пароль успешно изменен. Вам потребуется войти заново.');
      await logout();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Ошибка при смене пароля.');
    } finally {
      setIsChangingPassword(false);
    }
  };

  const handleRevokeAllSessions = async () => {
    if (!confirm('Вы уверены, что хотите завершить все остальные сессии?')) return;
    setIsRevoking(true);
    try {
      await userService.revokeAllSessions();
      alert('Все остальные сессии завершены');
      await loadSessions();
    } catch (err) {
      alert('Ошибка при завершении сессий');
    } finally {
      setIsRevoking(false);
    }
  };

  if (!user) return <div className="p-8 text-subtle">Загрузка...</div>;

  const tabs = [
    { id: 'profile', label: 'Профиль', icon: User },
    { id: 'security', label: 'Безопасность', icon: Shield },
    { id: 'sessions', label: 'Активные сессии', icon: Monitor },
  ] as const;

  return (
    <div className="max-w-4xl mx-auto p-4 md:p-8">
      <h1 className="text-4xl font-light text-primary mb-8 tracking-tight">Настройки профиля</h1>

      {/* Табы */}
      <div className="flex border-b border-border mb-8">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center gap-2 px-4 py-3 text-sm font-medium border-b-2 transition-colors -mb-px ${
              activeTab === tab.id
                ? 'border-border-focus text-primary'
                : 'border-transparent text-secondary hover:text-primary hover:border-subtle'
            }`}
          >
            <tab.icon size={18} />
            {tab.label}
          </button>
        ))}
      </div>

      {/* Контент табов */}
      <div className="bg-surface rounded-lg border border-border p-6 md:p-8">
        {activeTab === 'profile' && (
          <ProfileTab
            user={user}
            displayName={displayName}
            setDisplayName={setDisplayName}
            onSave={handleSaveProfile}
            isSaving={isSavingProfile}
          />
        )}
        
        {activeTab === 'security' && (
          <SecurityTab
            onSubmit={handleChangePassword}
            isChanging={isChangingPassword}
          />
        )}
        
        {activeTab === 'sessions' && (
          <SessionsTab
            sessions={sessions}
            onRevokeAll={handleRevokeAllSessions}
            isRevoking={isRevoking}
          />
        )}
      </div>

      <ChangeEmailModal 
        isOpen={isChangeEmailOpen} 
        onClose={() => setIsChangeEmailOpen(false)} 
      />
    </div>
  );
}