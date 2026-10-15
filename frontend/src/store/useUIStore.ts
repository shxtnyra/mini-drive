// stores/useUIStore.ts
import { create } from 'zustand';
import type { NodeResponse } from '../types/api';

interface UIState {
  isCreateFolderOpen: boolean;
  openCreateFolder: () => void;
  closeCreateFolder: () => void;

  isRenameOpen: boolean;
  renameTarget: NodeResponse | null;
  openRenameModal: (target: NodeResponse) => void;
  closeRenameModal: () => void;

  moveFileUuids: string[];
  isMoveFilesOpen: boolean;
  openMoveFilesModal: (fileUuids: string[]) => void;
  closeMoveFilesModal: () => void;

  shareTarget: NodeResponse | null;
  isShareOpen: boolean;
  openShareModal: (target: NodeResponse) => void;
  closeShareModal: () => void;

  isVerifyEmailOpen: boolean;
  emailAction: 'initial' | 'change';
  openVerifyEmailModal: (action: 'initial' | 'change') => void;
  closeVerifyEmailModal: () => void;
}

export const useUIStore = create<UIState>((set) => ({
  isCreateFolderOpen: false,
  openCreateFolder: () => set({ isCreateFolderOpen: true }),
  closeCreateFolder: () => set({ isCreateFolderOpen: false }),

  isRenameOpen: false,
  renameTarget: null,
  openRenameModal: (target) => set({ isRenameOpen: true, renameTarget: target }),
  closeRenameModal: () => set({ isRenameOpen: false, renameTarget: null }),

  moveFileUuids: [],
  isMoveFilesOpen: false,
  openMoveFilesModal: (fileUuids) => set({ isMoveFilesOpen: true, moveFileUuids: fileUuids }),
  closeMoveFilesModal: () => set({ isMoveFilesOpen: false, moveFileUuids: [] }),

  shareTarget: null,
  isShareOpen: false,
  openShareModal: (target) => set({ isShareOpen: true, shareTarget: target }),
  closeShareModal: () => set({ isShareOpen: false, shareTarget: null }),

  isVerifyEmailOpen: false,
  emailAction: 'initial',
  openVerifyEmailModal: (action) => set({ isVerifyEmailOpen: true, emailAction: action }),
  closeVerifyEmailModal: () => set({ isVerifyEmailOpen: false, emailAction: 'initial' }),
}));