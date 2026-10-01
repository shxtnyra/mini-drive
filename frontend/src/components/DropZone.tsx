import { useState, useRef } from 'react';
import { Upload } from 'lucide-react';
import { useChunkedUpload } from '../hooks/useChunkedUpload';
import { useFileStore } from '../store/useFileStore';

interface DropZoneProps {
  children: React.ReactNode;
}

export default function DropZone({ children }: DropZoneProps) {
  const [isDragging, setIsDragging] = useState(false);
  const dragCounter = useRef(0);
  const { uploadFile } = useChunkedUpload();
  const currentFolder = useFileStore((state) => state.currentFolder);

  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    dragCounter.current++;
    if (e.dataTransfer.items && e.dataTransfer.items.length > 0) {
      setIsDragging(true);
    }
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    dragCounter.current--;
    if (dragCounter.current === 0) {
      setIsDragging(false);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    dragCounter.current = 0;

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const files = Array.from(e.dataTransfer.files);
      files.forEach((file) => {
        uploadFile(file, { parentUuid: currentFolder || undefined });
      });
    }
  };

  return (
    <div
      onDragEnter={handleDragEnter}
      onDragLeave={handleDragLeave}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
      className="relative min-h-full"
    >
      {isDragging && (
        <div className="absolute inset-0 z-50 flex items-center justify-center bg-surface/90 backdrop-blur-md border-2 border-dashed border-border-focus rounded-lg transition-all duration-200">
          <div className="text-center">
            <Upload className="mx-auto text-secondary mb-3" size={48} strokeWidth={1.5} />
            <p className="text-lg font-medium text-primary">
              Отпустите файлы для загрузки
            </p>
          </div>
        </div>
      )}
      {children}
    </div>
  );
}