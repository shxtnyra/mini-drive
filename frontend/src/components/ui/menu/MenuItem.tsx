import { ChevronRight } from 'lucide-react';
import { useRef, useState, useEffect } from 'react';

interface MenuItemProps {
  icon?: React.ReactNode;
  label: string;
  shortcut?: string;
  disabled?: boolean;
  danger?: boolean;
  onClick?: () => void;
  submenu?: React.ReactNode;
}

export default function MenuItem({
  icon,
  label,
  shortcut,
  disabled = false,
  danger = false,
  onClick,
  submenu,
}: MenuItemProps) {
  const [isSubmenuOpen, setIsSubmenuOpen] = useState(false);
  const [submenuPosition, setSubmenuPosition] = useState({ top: 0, left: 0 });
  const itemRef = useRef<HTMLButtonElement>(null);
  const closeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Очистка таймера при размонтировании
  useEffect(() => {
    return () => {
      if (closeTimer.current) clearTimeout(closeTimer.current);
    };
  }, []);

  const handleMouseEnter = () => {
    if (!submenu || !itemRef.current) return;

    // Отменяем закрытие, если курсор вернулся
    if (closeTimer.current) {
      clearTimeout(closeTimer.current);
      closeTimer.current = null;
    }

    const rect = itemRef.current.getBoundingClientRect();
    const submenuWidth = 240;

    // Позиционируем подменю справа от элемента
    let submenuX = rect.right;
    let submenuY = rect.top;

    // Если подменю выходит за правый край — показываем слева
    if (submenuX + submenuWidth > window.innerWidth) {
      submenuX = rect.left - submenuWidth;
    }

    setSubmenuPosition({ top: submenuY, left: submenuX });
    setIsSubmenuOpen(true);
  };

  const handleMouseLeave = () => {
    if (!submenu) return;

    // Запускаем таймер закрытия (200мс задержка)
    closeTimer.current = setTimeout(() => {
      setIsSubmenuOpen(false);
    }, 200);
  };

  const handleSubmenuEnter = () => {
    // При входе на подменю — отменяем закрытие
    if (closeTimer.current) {
      clearTimeout(closeTimer.current);
      closeTimer.current = null;
    }
    setIsSubmenuOpen(true);
  };

  const handleSubmenuLeave = () => {
    // При уходе с подменю — снова запускаем таймер
    closeTimer.current = setTimeout(() => {
      setIsSubmenuOpen(false);
    }, 200);
  };

  const handleClick = () => {
    if (disabled) return;
    onClick?.();
  };

  const baseClasses = 'w-full px-3.5 py-2 flex items-center justify-between transition-colors';
  
  const stateClasses = disabled
    ? 'opacity-50'
    : 'hover:bg-page cursor-pointer'

  return (
    <>
      <button
        ref={itemRef}
        onClick={handleClick}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        disabled={disabled}
        className={`${baseClasses} ${stateClasses}`}
      >
        <div className="flex items-center gap-3">
          {icon && (
            <span className={danger ? 'text-error-icon' : 'text-secondary'}>
              {icon}
            </span>
          )}
          <span>{label}</span>
        </div>
        {shortcut && (
          <span className="text-xs text-subtle font-mono">{shortcut}</span>
        )}
        {submenu && <ChevronRight size={16} className="text-subtle" />}
      </button>

      {/* Подменю */}
      {submenu && isSubmenuOpen && (
        <div
          style={{ top: submenuPosition.top, left: submenuPosition.left }}
          className="fixed z-50 w-60 bg-surface rounded-lg shadow-xl border border-border py-1.5 text-sm font-medium text-primary select-none animate-in fade-in slide-in-from-left-2 duration-100"
          onMouseEnter={handleSubmenuEnter}
          onMouseLeave={handleSubmenuLeave}
        >
          {submenu}
        </div>
      )}
    </>
  );
}