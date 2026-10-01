import { X } from 'lucide-react';

interface CheckboxProps {
  id: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  label: string;
}

export default function Checkbox({ id, checked, onChange, label }: CheckboxProps) {
  return (
    <label htmlFor={id} className="flex items-center gap-3 cursor-pointer group">
      <input
        type="checkbox"
        id={id}
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        className="peer sr-only"
      />
      
      <div className="w-5 h-5 rounded border border-gray-700 bg-transparent 
                      peer-checked:bg-white peer-checked:border-white 
                      flex items-center justify-center transition-all duration-200 
                      group-hover:border-gray-500">
        <X 
          className="text-black transition-opacity duration-200" 
          size={14} 
          strokeWidth={3} 
        />
      </div>
      
      <span className="text-gray-500 text-sm group-hover:text-gray-300 transition-colors select-none">
        {label}
      </span>
    </label>
  );
}