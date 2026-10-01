import { useFileStore } from '../store/useFileStore';
import { useUIStore } from '../store/useUIStore';
import { useChunkedUpload } from './useChunkedUpload';

export function useCreateActions() {
  const { uploadFile } = useChunkedUpload();
  const openCreateFolderModal = useUIStore((state) => state.openCreateFolder);
  const currentFolder = useFileStore((state) => state.currentFolder);

  const openFilePicker = () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.multiple = true;
    input.onchange = (e: any) => {
      const files = e.target.files;
      if (files) {
        Array.from(files).forEach((file: any) => uploadFile(file, {parentUuid: currentFolder}));
      }
    };
    input.click();
  };

  const openFolderPicker = () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.multiple = true;
    input.webkitdirectory = true;
    input.onchange = (e: any) => {
      const files = e.target.files;
      if (files) {
        Array.from(files).forEach((file: any) => uploadFile(file));
      }
    };
    input.click();
  };

  return { openFilePicker, openFolderPicker, openCreateFolderModal };
}