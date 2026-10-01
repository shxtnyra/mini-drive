import { useLocation, useNavigate } from "react-router-dom";
import { useAuthStore } from "../../store/useAuthStore";
import { HardDrive, LogOut, Star, Trash2, UserIcon, Users } from "lucide-react";
import { formatBytes } from "../../utils/formatters";
import Logo from "../icon/Logo";
import ProgressBar from "../ui/ProgressBar";

export default function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((state) => state.user);

  const quotaPercent = user ? Math.min((user.usedBytes / user.quotaBytes) * 100, 100) : 0;
  const isQuotaCritical = quotaPercent > 90;

  function NavItem({
    icon,
    label,
    active,
    onClick,
  }: {
    icon: React.ReactNode;
    label: string;
    active?: boolean;
    onClick?: () => void;
  }) {
    return (
      <button
        onClick={onClick}
        className={`group relative flex items-center gap-3 px-3 py-2.5 w-full text-left transition-all duration-200 cursor-pointer rounded-lg ${
          active
            ? 'bg-sidebar-active text-primary'
            : 'text-secondary hover:bg-sidebar-hover hover:text-primary'
        }`}
      >
        {/* Акцентная полоска слева для активного элемента */}
        {active && (
          <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-6 bg-primary rounded-r-full"></div>
        )}
        
        {/* Иконка с анимацией (цвет наследуется от кнопки: text-primary или text-secondary) */}
        <div className={`transition-transform duration-200 ${active ? 'scale-110' : 'group-hover:scale-105'}`}>
          {icon}
        </div>
        
        <span className={`text-sm font-medium ${active ? 'font-semibold' : ''}`}>
          {label}
        </span>
      </button>
    );
  }

  return (
    <aside className="w-64 bg-sidebar flex flex-col h-full border-r border-sidebar">
      {/* Логотип */}
      <div className="flex items-center gap-3 px-5 py-6 border-b border-sidebar">
        {/* Цвет логотипа привязан к CSS-переменной, как в RegisterPage */}
        <Logo color="var(--color-primary)" size={36} />
        <h1 className="text-primary text-lg font-semibold tracking-wide">Mini Drive</h1>
      </div>

      {/* Навигация */}
      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        <NavItem 
          icon={<HardDrive size={20} />} 
          label="Мой диск" 
          active={location.pathname === '/drive/my-drive' || location.pathname.startsWith('/drive/folders/')} 
          onClick={() => navigate('/drive/my-drive')} 
        />
        <NavItem 
          icon={<Users size={20} />} 
          label="Общий доступ" 
          active={location.pathname === '/drive/shared'} 
          onClick={() => navigate('/drive/shared')} 
        />
        <NavItem 
          icon={<Star size={20} />} 
          label="Избранное" 
          active={location.pathname === '/drive/favorite'} 
          onClick={() => navigate('/drive/favorite')} 
        />
        <NavItem 
          icon={<Trash2 size={20} />} 
          label="Корзина" 
          active={location.pathname === '/drive/trash'} 
          onClick={() => navigate('/drive/trash')} 
        />

        {/* Разделитель */}
        <div className="pt-4 mt-4 border-t border-sidebar">
          <NavItem 
            icon={<UserIcon size={20} />} 
            label="Профиль" 
            active={location.pathname === '/drive/profile'} 
            onClick={() => navigate('/drive/profile')} 
          />
          <NavItem 
            icon={<LogOut size={20} />} 
            label="Выйти" 
            onClick={() => useAuthStore.getState().logout()} 
          />
        </div>
      </nav>

      {/* Квота и пользователь */}
      <div className="px-4 py-4 border-t border-sidebar space-y-4">
        {/* Квота хранилища */}
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <p className="text-xs text-secondary">Хранилище</p>
            <p className="text-xs font-medium text-primary">
              {Math.round(quotaPercent)}%
            </p>
          </div>
          
          <ProgressBar 
            value={quotaPercent} 
            variant={isQuotaCritical ? 'critical' : 'default'}
          />
          
          <p className="text-xs text-secondary">
            {user ? formatBytes(user.usedBytes) : '0 Б'} из {user ? formatBytes(user.quotaBytes) : '0 Б'}
          </p>
        </div>
      </div>
    </aside>
  );
}