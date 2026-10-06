import { useCallback, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { fileService } from '../api/fileService';
import { useFileStore } from '../store/useFileStore';
import type { FileListMode } from '../config/fileListConfig';
import { buildFileQueryParams } from '../utils/fileQueries';

interface UseNodeListProps {
  context: 'DRIVE' | 'TRASH' | 'SHARED' | 'FAVORITE';
  mode: FileListMode;
}

export function useNodeList({ context, mode }: UseNodeListProps) {
  const { folderId } = useParams<{ folderId?: string }>();
  const currentFolder = folderId || null;

  const { 
    setFiles, appendFiles, clearSelection, setBreadcrumbs, 
    setLoading, setError, isLoading, nextCursor, hasMore,
    sortBy, sortDirection, filters
  } = useFileStore();

  const loadFiles = useCallback(async () => {
    setLoading(true);
    setError(false);
    setFiles([]);
    clearSelection();

    try {
      const params = buildFileQueryParams(context, currentFolder, filters, sortBy, sortDirection);
      const response = await fileService.getFiles(params);
      
      setFiles(response.files.items);
      
      let breadcrumbs = response.breadcrumbs;
      setBreadcrumbs(breadcrumbs);
      
      useFileStore.setState({ 
        currentFolder,
        nextCursor: response.files.nextCursor, 
        hasMore: response.files.hasNext 
      });
      
    } catch (error) {
      setError(true);
      console.error('Ошибка загрузки файлов:', error);
    } finally {
      setLoading(false);
    } 
  }, [
    context, currentFolder, mode, 
    sortBy, sortDirection, filters,
    setFiles, setLoading, setError, clearSelection, setBreadcrumbs
  ]);

  const loadMore = useCallback(async () => {
    if (isLoading || !hasMore || !nextCursor) return;
    
    setLoading(true);
    try {
      const params = buildFileQueryParams(context, currentFolder, filters, sortBy, sortDirection);
      params.cursorUuid = nextCursor.uuid;
      params.cursorValue = nextCursor.value;

      const response = await fileService.getFiles(params);
      appendFiles(response.files.items, response.files.nextCursor, response.files.hasNext);
    } catch (error) {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, [
    context, currentFolder, filters, sortBy, sortDirection, 
    nextCursor, hasMore, isLoading, appendFiles, setLoading, setError
  ]);

  useEffect(() => {
    loadFiles();
  }, [loadFiles]);

  return { loadMore, refresh: loadFiles };
}