import { create } from 'zustand';
import type { AddDownloadParams, TransferTask } from '../types/ui';

export type TransferType = 'upload' | 'download' | 'archiving';
export type TransferStatus = 'pending' | 'uploading' | 'completed' | 'error' | 'cancelled';


interface TransferState {
  transfers: TransferTask[];
  
  addUpload: (file: File) => string;
  updateUpload: (id: string, updates: Partial<TransferTask>) => void;
  removeUpload: (id: string) => void;
  cancelUpload: (id: string) => void;
  addDownload: (params: AddDownloadParams) => string;
  clearFinished: () => void;
}

export const useTransferStore = create<TransferState>((set) => ({
  transfers: [],

  addUpload: (file: File) => {
    const id = crypto.randomUUID();
    const newTask: TransferTask = {
      id,
      name: file.name,
      file,
      type: 'upload',
      progress: 0,
      status: 'pending',
      uploadedBytes: 0,
      totalBytes: file.size,
      startTime: Date.now(),
    };

    set((state) => ({
      transfers: [...state.transfers, newTask],
    }));

    return id;
  },

  addDownload: ({ id = crypto.randomUUID(), name, totalBytes = 0, type = 'download', abortController }: AddDownloadParams) => {
    const newTask: TransferTask = {
      id,
      name,
      type,
      progress: 0,
      status: 'pending',
      uploadedBytes: 0,
      totalBytes,
      startTime: Date.now(),
      abortController,
    };

    set((state) => ({
      transfers: [...state.transfers, newTask],
    }));

    return id;
  },

  updateUpload: (id: string, updates: Partial<TransferTask>) => {
    set((state) => ({
      transfers: state.transfers.map((task) =>
        task.id === id ? { ...task, ...updates } : task
      ),
    }));
  },

  removeUpload: (id: string) => {
    set((state) => ({
      transfers: state.transfers.filter((task) => task.id !== id),
    }));
  },

  cancelUpload: (id: string) => {
    set((state) => ({
      transfers: state.transfers.map((task) => {
        if (task.id === id) {
          task.abortController?.abort();
          return { ...task, status: 'cancelled' };
        }
        return task;
      }),
    }));
  },

  clearFinished: () => {
    set((state) => ({
      transfers: state.transfers.filter(
        (t) => t.status === 'uploading' || t.status === 'pending'
      ),
    }));
  },
}));

export const useUploadStore = useTransferStore;