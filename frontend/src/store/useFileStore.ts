import { create } from 'zustand';
import type { BreadcrumbItem, CursorDto, NodeResponse } from '../types/api';
import type { FileFilters } from '../types/ui';

interface FileState {
  currentFolder: string | null; 
  files: NodeResponse[];
  breadcrumbs: BreadcrumbItem[];
  
  isLoading: boolean;
  nextCursor: CursorDto | null;
  hasMore: boolean;
  isError: boolean,
  
  viewMode: 'list' | 'grid';

  selectedUuids: string[];
  lastSelectedUuid: string | null;

  sortBy: string;
  sortDirection: 'ASC' | 'DESC'
  setSort: (sortBy: string, sortDirection: 'ASC' | 'DESC') => void;

  filters: FileFilters;
  setFilters: (newFilters: Partial<FileFilters>) => void;
  clearFilters: () => void;

  setFiles: (files: NodeResponse[]) => void;
  updateFileInList: (updatedFile: NodeResponse) => void;
  appendFiles: (newFiles: NodeResponse[], nextCursor: CursorDto | null, hasNext: boolean) => void;
  addFile: (file: NodeResponse) => void;
  removeFiles: (fileUuids: string[]) => void;
  setBreadcrumbs: (breadcrumbs: BreadcrumbItem[]) => void;
  setViewMode: (mode: 'list' | 'grid') => void;
  setLoading: (loading: boolean) => void;
  setError: (isError: boolean) => void;

  selectSingle: (uuid: string) => void;
  selectRange: (targetUuid: string) => void;
  toggleSelect: (uuid: string) => void;
  updateFilesFavoriteStatus: (fileUuids: string[], isFavorite: boolean) => void;
  clearSelection: () => void;
}

export const useFileStore = create<FileState>((set, get) => ({
  currentFolder: null,
  files: [],
  breadcrumbs: [],
  isLoading: false,
  nextCursor: null,
  hasMore: true,
  isError: false,
  viewMode: 'list',
  selectedUuids: [],
  lastSelectedUuid: null,
  sortBy: 'createdAt',
  sortDirection: 'DESC',
  filters: {
    nodeType: undefined,
    mimeTypeGroup: 'all',
    dateRange: 'any',
  },

  setFiles: (files) => set({ files, selectedUuids: [], lastSelectedUuid: null, isError: false }),
  setSort: (sortBy, sortDirection) => set({ sortBy, sortDirection }),

  appendFiles: (newFiles, nextCursor, hasNext) =>
    set((state) => ({
      files: [...state.files, ...newFiles],
      nextCursor: nextCursor,
      hasMore: hasNext,
    })),

  addFile: (file) =>
    set((state) => ({
      files: [file, ...state.files],
    })),

  removeFiles: (fileUuids) =>
    set((state) => ({
      files: state.files.filter((f) => !fileUuids.includes(f.uuid)),
    })),

  setBreadcrumbs: (breadcrumbs) => set({ breadcrumbs }),

  setViewMode: (mode) => set({ viewMode: mode }),

  setLoading: (loading) => set({ isLoading: loading }),

  setError: (isError) => set({ isError }),

  selectSingle: (uuid) => set({selectedUuids: [uuid], lastSelectedUuid: uuid}),

  updateFileInList: (updatedFile: NodeResponse) => {
  set((state) => ({
    files: state.files.map((file) =>
      file.uuid === updatedFile.uuid ? updatedFile : file),
    }));
  },

  updateFilesFavoriteStatus: (fileUuids: string[], isFavorite: boolean) => {
    set((state) => ({
      files: state.files.map((file) =>
        fileUuids.includes(file.uuid) ? { ...file, isFavorite } : file
      ),
    }));
  },

  selectRange: (targetUuid) => {
    const { files, lastSelectedUuid, selectedUuids } = get();
    if (!lastSelectedUuid) {
      set({ selectedUuids: [targetUuid], lastSelectedUuid: targetUuid });
      return;
    }

    const lastIndex = files.findIndex((f) => f.uuid === lastSelectedUuid);
    const targetIndex = files.findIndex((f) => f.uuid === targetUuid);

    if (lastIndex === -1 || targetIndex === -1) return;

    const start = Math.min(lastIndex, targetIndex);
    const end = Math.max(lastIndex, targetIndex);

    const rangeUuids = files.slice(start, end + 1).map((f) => f.uuid);

    set({
      selectedUuids: Array.from(new Set([...selectedUuids, ...rangeUuids])),
    });
  },

  toggleSelect: (uuid: string) => {
    const { selectedUuids } = get();
    const exists = selectedUuids.includes(uuid);

    if (exists){
      set({
        selectedUuids: selectedUuids.filter((id) => id !== uuid),
      })
    } 
    else {
      set({
        selectedUuids: [...selectedUuids, uuid],
        lastSelectedUuid: uuid,
      });
    }
  },

  clearSelection: () => set({ selectedUuids: [], lastSelectedUuid: null }),

  setFilters: (newFilters) => 
    set((state) => ({ 
      filters: { ...state.filters, ...newFilters } 
    })),
  clearFilters: () => 
    set({ 
      filters: { nodeType: undefined, mimeTypeGroup: 'all', dateRange: 'any' } 
    }),
}));