import { useFileStore } from '../store/useFileStore';
import { useUIStore } from '../store/useUIStore';
import { useChunkedUpload } from './useChunkedUpload';
import { uploadFolderStructure } from '../utils/folderUpload';

export function useCreateActions() {
  const { uploadFile } = useChunkedUpload();
  const openCreateFolderModal = useUIStore((state) => state.openCreateFolder);
  const currentFolder = useFileStore((state) => state.currentFolder);

  const openFilePicker = () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.multiple = true;
    input.onchange = () => {
      const files = Array.from(input.files ?? []);
      input.remove();
      files.forEach((file) => {
        void uploadFile(file, { parentUuid: currentFolder });
      });
    };
    input.click();
  };

  const openFolderPicker = () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.multiple = true;
    input.setAttribute('webkitdirectory', '');
    input.onchange = () => {
      const files = Array.from(input.files ?? []);
      input.remove();
      void uploadFolderStructure(files, currentFolder, uploadFile)
        .catch((error: unknown) => {
          console.error('Не удалось подготовить структуру загружаемой папки:', error);
        });
    };
    input.click();
  };

  return { openFilePicker, openFolderPicker, openCreateFolderModal };
}