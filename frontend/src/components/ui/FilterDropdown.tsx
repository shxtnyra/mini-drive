import { ChevronDown } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';

interface FilterOption {
  value: string;
  label: string;
  icon?: React.ReactNode;
}

interface FilterDropdownProps {
  label: string;
  isActive: boolean;
  options: FilterOption[];
  selectedValue: string;
  onSelect: (value: string) => void;
}

export default function FilterDropdown({
  label,
  isActive,
  options,
  selectedValue,
  onSelect,
}: FilterDropdownProps) {
  const [isOpen, setIsOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const activeOption = options.find((o) => o.value === selectedValue);

  return (
    <div className="relative" ref={ref}>
      {/* Кнопка-триггер */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={`flex items-center gap-2 px-4 py-1.5 text-sm font-medium rounded-lg border transition-colors cursor-pointer ${
          isActive
            ? 'bg-selection-bg text-primary border-selection-border'
            : 'bg-surface hover:bg-sidebar-hover text-primary border-border'
        }`}
      >
        <span>{label}</span>
        {activeOption?.icon && (
          <span className="text-secondary">{activeOption.icon}</span>
        )}
        <ChevronDown
          size={14}
          className={`text-secondary transition-transform ${
            isOpen ? 'rotate-180' : ''
          }`}
        />
      </button>

      {/* Выпадающее меню */}
      {isOpen && (
        <div className="absolute top-full w-56 bg-surface border border-border shadow-xl z-50">
          {options.map((option) => (
            <button
              key={option.value}
              onClick={() => {
                onSelect(option.value);
                setIsOpen(false);
              }}
              className={`w-full flex items-center gap-2 px-3 py-2 text-sm text-left transition-colors cursor-pointer ${
                selectedValue === option.value
                  ? 'bg-selection-bg hover:bg-sidebar-active font-medium'
                  : 'text-primary hover:bg-sidebar-active'
              }`}
            >
              {option.icon && (
                <span className="text-secondary">{option.icon}</span>
              )}
              {option.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}