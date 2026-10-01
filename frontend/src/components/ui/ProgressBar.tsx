interface ProgressBarProps {
  value: number; // Процент от 0 до 100
  variant?: 'default' | 'critical';
  rows?: number;
  columns?: number;
  className?: string;
}

export default function ProgressBar({
  value,
  variant = 'default',
  rows = 3,
  columns = 30,
  className = '',
}: ProgressBarProps) {
  const normalizedValue = Math.min(Math.max(value, 0), 100);
  
  const totalBlocks = rows * columns;
  const filledBlocks = Math.round((normalizedValue / 100) * totalBlocks);
  
  // Сколько полных столбцов заполнено
  const fullColumns = Math.floor(filledBlocks / rows);
  // Сколько дополнительных кубиков в следующем столбце (остаток)
  const remainder = filledBlocks % rows;

  return (
    <div
      className={`w-full flex flex-col gap-0.5 p-0.5 ${className}`}
      role="progressbar"
      aria-valuenow={normalizedValue}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-label={`Заполнено хранилище на ${normalizedValue}%`}
    >
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <div key={rowIndex} className="flex gap-0.5 w-full">
          {Array.from({ length: columns }).map((_, colIndex) => {
            // Столбец полностью заполнен
            const isFullColumn = colIndex < fullColumns;
            // Текущий столбец - это тот самый с остатком
            const isRemainderColumn = colIndex === fullColumns;
            // Этот конкретный кубик в столбце с остатком должен быть заполнен
            const isRemainderBlock = isRemainderColumn && rowIndex < remainder;
            
            const isActive = isFullColumn || isRemainderBlock;

            return (
              <div
                key={`${rowIndex}-${colIndex}`}
                className={`flex-1 aspect-square ransition-colors duration-150 ${
                  isActive
                    ? variant === 'critical'
                      ? 'bg-red-500 shadow-[0_0_6px_rgba(239,68,68,0.5)]'
                      : 'bg-white'
                    : 'bg-white/15'
                }`}
              />
            );
          })}
        </div>
      ))}
    </div>
  );
}