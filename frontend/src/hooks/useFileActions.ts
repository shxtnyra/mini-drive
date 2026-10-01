import { useCallback } from 'react';
import { fileService } from '../api/fileService';
import { useFileStore } from '../store/useFileStore';
import type { NodeResponse } from '../types/api';
import { useUIStore } from '../store/useUIStore';

const triggerBlobDownload = (blob: Blob, filename: string) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

/**
 * Хук для выполнения действий над файлами (мутаций и скачивания).
 * Отвечает за: удаление, восстановление, скачивание одиночных файлов и архивов.
 * Гарантирует синхронизацию API и локального стейта.
 */
export function useFileActions() {
  const currentFolder = useFileStore((state) => state.currentFolder)
  const files = useFileStore((state) => state.files);
  const addFile = useFileStore((state) => state.addFile);
  const updateFileInList = useFileStore((state) => state.updateFileInList);
  const removeFiles = useFileStore((state) => state.removeFiles);
  const clearSelection = useFileStore((state) => state.clearSelection);
  const openRenameModal = useUIStore((state) => state.openRenameModal);


  const createFolder = useCallback(
    async (name: string): Promise<NodeResponse> => {
      const newFolder = await fileService.createFolder(name, currentFolder);
      addFile(newFolder);
      return newFolder;
    },
    [currentFolder, addFile]
  );

  const renameFile = useCallback(
    async (fileUuid: string, newName: string): Promise<NodeResponse> => {
      const updatedFile = await fileService.updateFile(fileUuid, newName);
      updateFileInList(updatedFile);
      return updatedFile;
    },
    [updateFileInList]
  );

  const deleteFiles = useCallback(
    async (uuids: string[]) => {
      if (!uuids || uuids.length === 0) return;

      try {
        await fileService.deleteFiles(uuids);

        removeFiles(uuids);
        clearSelection();

        // TODO: Добавить showToast('Файлы перемещены в корзину')
      } catch (error) {
        console.error('Ошибка при удалении файлов:', error);
        // TODO: Добавить showToast('Не удалось удалить файлы', 'error')
      }
    },
    [removeFiles, clearSelection]
  );

  const restoreFiles = useCallback(
    async (uuids: string[]) => {
      if (!uuids || uuids.length === 0) return;

      try {
        await fileService.restoreFiles(uuids);
        removeFiles(uuids);
        clearSelection();
        // TODO: showToast('Файлы восстановлены')
      } catch (error) {
        console.error('Ошибка при восстановлении:', error);
      }
    },
    [removeFiles, clearSelection]
  );

  const downloadFile = useCallback(
    async (uuid: string, fileName?: string) => {
      try {
        // Если имя файла не передано явно, ищем его в файловом сторе
        const targetFile = files.find((f) => f.uuid === uuid);
        const nameToSave = fileName || targetFile?.filename || 'file';

        const blob = await fileService.downloadFile(uuid);
        triggerBlobDownload(blob, nameToSave);
      } catch (error) {
        console.error('Ошибка при скачивании файла:', error);
        // TODO: showToast('Не удалось скачать файл', 'error')
      }
    },
    [files]
  );

  const downloadZip = useCallback(async (uuids: string[], archiveName = 'archive.zip') => {
    if (!uuids || uuids.length === 0) return;

    try {
      const blob = await fileService.downloadZip(uuids);
      triggerBlobDownload(blob, archiveName);
    } catch (error) {
      console.error('Ошибка при скачивании ZIP-архива:', error);
      // TODO: showToast('Не удалось скачать архив', 'error')
    }
  }, []);

  /**
   * Универсальный метод скачивания:
   * - Если выделен 1 элемент и это ФАЙЛ -> скачивает файл напрямую.
   * - Если выделена 1 ПАПКА или МНОГО элементов -> скачивает как ZIP-архив.
   */
  const downloadSelected = useCallback(
    async (uuids: string[]) => {
      if (!uuids || uuids.length === 0) return;

      if (uuids.length === 1) {
        const uuid = uuids[0];
        const item = files.find((f) => f.uuid === uuid);

        if (item && item.fileType === 'FILE') {
          await downloadFile(item.uuid, item.filename);
          return;
        }
      }

      const zipName =
        uuids.length === 1
          ? `${files.find((f) => f.uuid === uuids[0])?.filename || 'folder'}.zip`
          : 'files.zip';

      await downloadZip(uuids, zipName);
    },
    [files, downloadFile, downloadZip]
  );

  return {
    deleteFiles,
    createFolder,
    renameFile,
    restoreFiles,
    downloadFile,
    downloadZip,
    downloadSelected,
    openRenameModal
  };
}