import { api } from './client';
import type { BeginUploadRequest, BeginUploadResponse, NodeResponse } from '../types/api';

export const uploadService = {
  async initiateUpload(data: BeginUploadRequest): Promise<BeginUploadResponse> {
    const response = await api.post('/files/upload/initiate', data);
    return response.data;
  },

  async uploadChunk(
    sessionUuid: string,
    partNumber: number,
    chunk: Blob,
    onProgress?: (progress: number) => void
  ): Promise<void> {
    const formData = new FormData();
    formData.append('file', chunk);

    await api.post(`/files/upload/${sessionUuid}/chunks`, formData, {
      params: { partNumber },
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

  async getUploadStatus(sessionUuid: string): Promise<{
    sessionUuid: string;
    uploadedParts: number[];
    totalParts: number;
  }> {
    const response = await api.get(`/files/upload/${sessionUuid}/status`);
    return response.data;
  },
};