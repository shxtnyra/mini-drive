import { api } from './client';
import type {
  BeginUploadRequest,
  BeginUploadResponse,
  DirectUploadRequest,
  NodeResponse,
  UploadStatusResponse,
} from '../types/api';

export const uploadService = {
  async uploadDirect(
    metadata: DirectUploadRequest,
    file: File,
    onProgress?: (progress: number) => void,
    signal?: AbortSignal
  ): Promise<NodeResponse> {
    const formData = new FormData();
    formData.append(
      'metadata',
      new Blob([JSON.stringify(metadata)], { type: 'application/json' })
    );
    formData.append('file', file);

    const response = await api.post<NodeResponse>('/files/upload', formData, {
      signal,
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          onProgress(Math.round((progressEvent.loaded * 100) / progressEvent.total));
        }
      },
    });
    return response.data;
  },

  async initiateUpload(data: BeginUploadRequest): Promise<BeginUploadResponse> {
    const response = await api.post<BeginUploadResponse>('/files/upload/initiate', data);
    return response.data;
  },

  async uploadChunk(
    sessionUuid: string,
    partNumber: number,
    chunk: Blob,
    onProgress?: (progress: number) => void,
    signal?: AbortSignal
  ): Promise<void> {
    const formData = new FormData();
    formData.append('file', chunk);

    await api.post(`/files/upload/${sessionUuid}/chunks`, formData, {
      params: { partNumber },
      signal,
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const percentCompleted = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
          onProgress(percentCompleted);
        }
      },
    });
  },

  async completeUpload(sessionUuid: string): Promise<NodeResponse> {
    const response = await api.post(`/files/upload/${sessionUuid}:complete`);
    return response.data;
  },

  async abortUpload(sessionUuid: string): Promise<void> {
    await api.delete(`/files/upload/${sessionUuid}:abort`);
  },

  async getUploadStatus(sessionUuid: string): Promise<UploadStatusResponse> {
    const response = await api.get<UploadStatusResponse>(`/files/upload/${sessionUuid}/status`);
    return response.data;
  },
};