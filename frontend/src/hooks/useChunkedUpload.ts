import { useCallback } from 'react';
import { uploadService } from '../api/uploadService';
import { useUploadStore } from '../store/useTransferStore';
import { useFileStore } from '../store/useFileStore';

const MIN_CHUNK_SIZE = 5 * 1024 * 1024; // 5 MB
const MAX_CHUNK_SIZE = 50 * 1024 * 1024; // 50 MB
const DEFAULT_CHUNK_SIZE = 10 * 1024 * 1024; // 10 MB
const DIRECT_UPLOAD_LIMIT = 50 * 1024 * 1024;

interface ChunkedUploadOptions {
  parentUuid?: string | null;
  chunkSize?: number;
}

/**
 * Хук для управления сложной процессом загрузки файлов по частям (chunked upload).
 * Отвечает за: инициацию, нарезку, отправку чанков, обработку прогресса и отмену.
 */
export function useChunkedUpload() {
  const { addUpload, updateUpload, cancelUpload } = useUploadStore();
  const addFileToStore = useFileStore((state) => state.addFile);

  const uploadFile = useCallback(
    async (file: File, options: ChunkedUploadOptions = {}, existingTaskId?: string) => {
      const { parentUuid, chunkSize = DEFAULT_CHUNK_SIZE } = options;
      const taskId = existingTaskId || addUpload(file, parentUuid, chunkSize);
      
      const validChunkSize = Math.max(MIN_CHUNK_SIZE, Math.min(MAX_CHUNK_SIZE, chunkSize));
      const abortController = new AbortController();
      updateUpload(taskId, {
        status: 'uploading',
        parentUuid,
        chunkSize: validChunkSize,
        abortController,
        sessionUuid: undefined,
        error: undefined,
        progress: 0,
        uploadedBytes: 0,
      });

      try {
        if (file.size < DIRECT_UPLOAD_LIMIT) {
          const uploadedFile = await uploadService.uploadDirect(
            {
              fileName: file.name,
              mimeType: file.type,
              parentUuid,
            },
            file,
            (progress) => {
              updateUpload(taskId, {
                progress,
                uploadedBytes: (file.size * progress) / 100,
              });
            },
            abortController.signal
          );

          if ((uploadedFile.parentUuid ?? null) === useFileStore.getState().currentFolder) {
            addFileToStore(uploadedFile);
          }
          updateUpload(taskId, {
            status: 'completed',
            progress: 100,
            uploadedBytes: file.size,
            abortController: undefined,
          });
          return;
        }

        // 1. Инициация загрузки на бэкенде
        const initiateResponse = await uploadService.initiateUpload({
          fileName: file.name,
          fileSize: file.size,
          mimeType: file.type,
          parentUuid,
          requestedChunkSize: validChunkSize,
        });

        const sessionUuid = initiateResponse.sessionUuid;
        updateUpload(taskId, { sessionUuid });

        // 2. Цикл загрузки чанков
        const actualChunkSize = initiateResponse.chunkSize;
        const totalChunks = initiateResponse.totalParts;
        let uploadedBytes = 0;

        for (let partNumber = 1; partNumber <= totalChunks; partNumber++) {
          const currentTask = useUploadStore.getState().transfers.find((u) => u.id === taskId);
          
          // Проверка на отмену пользователем
          if (abortController.signal.aborted || currentTask?.status === 'cancelled') {
            await uploadService.abortUpload(sessionUuid);
            return;
          }

          // Вырезаем чанк из файла
          const start = (partNumber - 1) * actualChunkSize;
          const end = Math.min(start + actualChunkSize, file.size);
          const chunk = file.slice(start, end);

          // Загружаем чанк
          await uploadService.uploadChunk(sessionUuid, partNumber, chunk,
            (chunkProgress) => {
              const chunkBytesUploaded = (chunk.size * chunkProgress) / 100;
              const totalUploadedBytes = uploadedBytes + chunkBytesUploaded;
              const overallProgress = (totalUploadedBytes / file.size) * 100;

              updateUpload(taskId, {
                progress: overallProgress,
                uploadedBytes: totalUploadedBytes,
              });
            },
            abortController.signal
          );

          uploadedBytes += chunk.size;
        }

        // 3. Завершаем загрузку
        const uploadedFile = await uploadService.completeUpload(sessionUuid);
        if ((uploadedFile.parentUuid ?? null) === useFileStore.getState().currentFolder) {
          addFileToStore(uploadedFile);
        }

        updateUpload(taskId, {
          status: 'completed',
          progress: 100,
          uploadedBytes: file.size,
          abortController: undefined,
        });

      } catch (error) {
        const currentTask = useUploadStore.getState().transfers.find((task) => task.id === taskId);
        if (abortController.signal.aborted || currentTask?.status === 'cancelled') return;
        console.error('Ошибка загрузки:', error);
        updateUpload(taskId, {
          status: 'error',
          error: error instanceof Error ? error.message : 'Unknown error',
          abortController: undefined,
        });
      }
    },
    [addUpload, updateUpload, addFileToStore]
  );

  const cancelFileUpload = useCallback(
    async (taskId: string) => {
      const task = useUploadStore.getState().transfers.find((u) => u.id === taskId);
      if (!task) return;

      cancelUpload(taskId);

      if (task.sessionUuid) {
        try {
          await uploadService.abortUpload(task.sessionUuid);
        } catch (error) {
          console.error('Failed to abort upload:', error);
        }
      }
    },
    [cancelUpload]
  );

  const retryFileUpload = useCallback(
    (taskId: string) => {
      const task = useUploadStore.getState().transfers.find((u) => u.id === taskId);
      if (!task) return;
      if (!task.file) return;
      uploadFile(task.file, {
        parentUuid: task.parentUuid,
        chunkSize: task.chunkSize,
      }, taskId);
    },
    [uploadFile]
  );

  return { uploadFile, cancelFileUpload, retryFileUpload };
}