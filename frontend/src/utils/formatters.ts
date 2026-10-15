export const formatDate = (dateStr?: string): string => {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('ru-RU', { 
    day: 'numeric', 
    month: 'short', 
    year: 'numeric', 
    hour: '2-digit', 
    minute: '2-digit' 
  });
};

export const formatBytes = (bytes?: number): string => {
  if (bytes == null || bytes === 0) return '0 Б';
  if (!Number.isFinite(bytes) || bytes < 0) return '—';
  
  const k = 1024;
  const sizes = ['Б', 'КБ', 'МБ', 'ГБ', 'ТБ'];
  
  const i = Math.min(
    Math.floor(Math.log(bytes) / Math.log(k)),
    sizes.length - 1
  );
  
  const value = bytes / Math.pow(k, i);
  
  const formatted = value % 1 === 0 
    ? value.toString() 
    : value.toFixed(1).replace(/\.0$/, '');
  
  return `${formatted} ${sizes[i]}`;
};