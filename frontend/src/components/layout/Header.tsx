import { Search } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../../store/useAuthStore";

export default function Header() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const userInitial = user?.displayName?.charAt(0).toUpperCase() || user?.username?.charAt(0).toUpperCase() || 'U';

  return (
    <header className="h-16 flex items-center px-4 gap-4 bg-header shrink-0 border-b border-sidebar">
      {/* Поиск */}
      <div className="flex-1 max-w-2xl relative"> 
        <div className="flex gap-2 items-center bg-search-bg rounded-2xl px-4 py-2.5 focus-within:bg-search-bg-focus focus-within:ring-2 focus-within:ring-border-focus transition-all duration-200">
          <Search size={20} className="text-secondary shrink-0" />
          <input
            type="text"
            placeholder="Поиск в диске"
            className="w-full bg-transparent border-none outline-none text-primary placeholder-subtle text-sm"
          />
        </div>
      </div>

      {/* Профиль пользователя */}
      <button
        onClick={() => navigate('/drive/profile')}
        className="flex items-center gap-2 p-1 pr-3 rounded-2xl ml-auto cursor-pointer hover:bg-sidebar-hover transition-colors"
        title="Открыть профиль"
      >
        <div className="w-8 h-8 bg-avatar-bg text-avatar-text rounded-full flex items-center justify-center font-semibold text-sm border border-avatar-border shrink-0">
          {userInitial}
        </div>
        <span className="text-sm font-semibold text-primary hidden md:block">
          {user?.displayName || user?.username || 'Профиль'}
        </span>
      </button>
    </header>
  );
}