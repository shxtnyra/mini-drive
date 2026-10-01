import type { GetFilesParams } from '../api/fileService';
import type { FileFilters } from '../types/ui';


export function getMimeTypeFilter(mimeTypeGroup?: string): string | undefined {
  if (!mimeTypeGroup || mimeTypeGroup === 'all') return undefined;

  const mimeMap: Record<string, string> = {
    image: 'image/%',
    video: 'video/%',
    audio: 'audio/%',
    document: 'application/%',
    archive: 'application/zip, application/x-rar-compressed, application/x-7z-compressed',
  };

  return mimeMap[mimeTypeGroup];
}

export function getFromDateFilter(dateRange?: string): string | undefined {
  if (!dateRange || dateRange === 'any') return undefined;

  const now = new Date();
  const fromDate = new Date();

  switch (dateRange) {
    case 'today':
      fromDate.setHours(0, 0, 0, 0);
      break;
    case 'week':
      fromDate.setDate(now.getDate() - 7);
      break;
    case 'month':
      fromDate.setMonth(now.getMonth() - 1);
      break;
    case 'year':
      fromDate.setFullYear(now.getFullYear() - 1);
      break;
  }

  return fromDate.toISOString();
}

export function buildFileQueryParams(
  context: "DRIVE" | "TRASH" | "SHARED" | "FAVORITE",
  currentFolder: string | null,
  filters: FileFilters,
  sortBy: string,
  sortDirection: 'ASC' | 'DESC'
): GetFilesParams {
  const params: GetFilesParams = {
    context,
    size: 20,
    sortBy,
    sortDirection,
  };

  if (currentFolder) {
    params.parentUuid = currentFolder;
  }

  if (filters.nodeType) {
    params.nodeType = filters.nodeType;
  }

  const mimeType = getMimeTypeFilter(filters.mimeTypeGroup);
  if (mimeType) {
    params.mimeType = mimeType;
  }

  const fromDate = getFromDateFilter(filters.dateRange);
  if (fromDate) {
    params.fromDate = fromDate;
  }

  return params;
}