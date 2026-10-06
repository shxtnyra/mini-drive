import { useEffect, useLayoutEffect, useRef, useState } from 'react';

interface MenuProps {
  x: number;
  y: number;
  onClose: () => void;
  children: React.ReactNode;
  width?: number;
}

export default function Menu({ x, y, onClose, children, width = 320 }: MenuProps) {
  const menuRef = useRef<HTMLDivElement>(null);
  const [position, setPosition] = useState({ top: y, left: x });

  // Корректировка позиции
  useLayoutEffect(() => {
    if (!menuRef.current) return;

    const { offsetWidth, offsetHeight } = menuRef.current;
    const screenWidth = window.innerWidth;
    const screenHeight = window.innerHeight;

    let correctedX = x;
    let correctedY = y;

    if (x + offsetWidth > screenWidth) {
      correctedX = x - offsetWidth;
    }

    if (y + offsetHeight > screenHeight) {
      correctedY = y - offsetHeight;
    }

    correctedX = Math.max(10, correctedX);
    correctedY = Math.max(10, correctedY);

    setPosition({ top: correctedY, left: correctedX });
  }, [x, y]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        onClose();
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [onClose]);

  return (
    <div
      ref={menuRef}
      style={{ top: position.top, left: position.left, width: `${width}px` }}
      className="fixed z-50 bg-surface rounded-lg shadow-xl border border-border py-1.5 text-sm font-medium text-primary select-none animate-in fade-in zoom-in-95 duration-100"
    >
      {children}
    </div>
  );
}