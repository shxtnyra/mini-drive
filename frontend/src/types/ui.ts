export interface FileFilters {
  nodeType?: 'FILE' | 'FOLDER';
  mimeTypeGroup?: 'image' | 'video' | 'audio' | 'document' | 'archive' | 'all';
  dateRange?: 'any' | 'today' | 'week' | 'month' | 'year';
}

export type TransferType = 'upload' | 'download' | 'archiving';
export type TransferStatus = 'pending' | 'uploading' | 'completed' | 'error' | 'cancelled';

export interface TransferTask {
  id: string;
  name: string;
  type: TransferType;
  totalBytes: number;
  progress: number;
  status: TransferStatus;
  uploadedBytes: number;
  startTime: number;
  file?: File;
  sessionUuid?: string;
  abortController?: AbortController;
  error?: string;
}

export interface AddDownloadParams {
  id?: string;
  name: string;
  totalBytes?: number;
  type?: 'download' | 'archiving';
  abortController?: AbortController;
}