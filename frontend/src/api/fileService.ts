import { api } from './client';
import type { NodeResponse, CursorResponse, FolderResponse } from '../types/api';

export interface GetFilesParams {
  context?: 'DRIVE' | 'TRASH' | 'SHARED' | 'FAVORITE';
  parentUuid?: string;
  nodeType?: 'FILE' | 'FOLDER';
  mimeType?: string;
  createdBy?: string,
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
  cursorUuid?: string;
  cursorValue?: string;
  size?: number;
  fromDate?: string;
}

export const fileService = {
  async getFiles(params: GetFilesParams = {}): Promise<FolderResponse> {
    const response = await api.get<FolderResponse>('/files', { params });
    return response.data;
  },

  async createFolder(
    name: string,
    parentUuid?: string | null
  ): Promise<NodeResponse> {
    const response = await api.post<NodeResponse>('/files/folders', {
      name,
      parentUuid: parentUuid || null,
    });
    return response.data;
  },

  async updateFile(
    fileUuid: string,
    newFileName: string
  ): Promise<NodeResponse> {
    const response = await api.patch<NodeResponse>(`/files/${fileUuid}`, {
      newFileName,
    });
    return response.data;
  },

  async deleteFiles(fileUuids: string[]): Promise<void> {
    await api.delete('/files', {
      data: { fileUuids },
    });
  },

  async restoreFiles(fileUuids: string[]): Promise<void> {
    await api.post('/files/restore', {
      fileUuids,
    });
  },

  async getTrash(
    cursor?: string,
    size: number = 50
  ): Promise<CursorResponse<NodeResponse>> {
    const params: Record<string, any> = { size };
    if (cursor) params.cursor = cursor;

    const response = await api.get<CursorResponse<NodeResponse>>('/files/trash', {
      params,
    });
    return response.data;
  },

  // Скачать одиночный файл
  async downloadFile(fileUuid: string): Promise<Blob> {
    const response = await api.get(`/files/${fileUuid}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },

  // Скачать файлы/папки в виде ZIP-архива
  async downloadZip(fileUuids: string[]): Promise<Blob> {
    const response = await api.post('/files/download-zip', { fileUuids }, {
      responseType: 'blob',
    });
    return response.data;
  },
};