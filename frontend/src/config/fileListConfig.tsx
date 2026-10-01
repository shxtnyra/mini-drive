import type { NodeResponse } from '../types/api';
import { FileNameCell } from '../utils/fileHelpers';
import { formatBytes, formatDate } from '../utils/formatters';

export type FileListMode = 'drive' | 'trash' | 'shared' | 'favorite';

export interface ColumnDef {
  key: string;
  label: string;
  className: string;
  render: (file: NodeResponse) => React.ReactNode;
}

export const DRIVE_COLUMNS: ColumnDef[] = [
  { key: 'name', label: 'Название', className: 'col-span-6', render: (f) => <FileNameCell file={f} /> },
  { key: 'owner', label: 'Владелец', className: 'col-span-2', render: () => 'Я' }, 
  { key: 'updatedAt', label: 'Дата изменения', className: 'col-span-2', render: (f) => formatDate(f.updatedAt || f.createdAt) },
  { key: 'size', label: 'Размер', className: 'col-span-2', render: (f) => f.fileType === 'FOLDER' ? '—' : formatBytes(f.sizeBytes) },
];

export const TRASH_COLUMNS: ColumnDef[] = [
  { key: 'name', label: 'Название', className: 'col-span-6', render: (f) => <FileNameCell file={f} /> },
  { key: 'deletedAt', label: 'Дата удаления', className: 'col-span-3', render: (f) => formatDate(f.deletedAt) },
  { key: 'size', label: 'Размер', className: 'col-span-3', render: (f) => f.fileType === 'FOLDER' ? '—' : formatBytes(f.sizeBytes) },
];

export const SHARED_COLUMNS: ColumnDef[] = [
  { key: 'name', label: 'Название', className: 'col-span-6', render: (f) => <FileNameCell file={f} /> },
  { key: 'owner', label: 'Владелец', className: 'col-span-3', render: (f) => f.ownerUuid ? 'Другой пользователь' : 'Я' },
  { key: 'updatedAt', label: 'Дата изменения', className: 'col-span-3', render: (f) => formatDate(f.updatedAt || f.createdAt) },
];

export const getColumnsByMode = (mode: FileListMode): ColumnDef[] => {
  switch (mode) {
    case 'trash': return TRASH_COLUMNS;
    case 'shared': return SHARED_COLUMNS;
    case 'favorite': return DRIVE_COLUMNS;
    default: return DRIVE_COLUMNS;
  }
};