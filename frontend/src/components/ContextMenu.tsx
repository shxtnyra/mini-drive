import { Download, Edit3, UserPlus, Folder, Info, Trash2, Star, FolderOutput, RotateCcw } from 'lucide-react';
import { useFileStore } from '../store/useFileStore';
import { useFileActions } from '../hooks/useFileActions';
import type { FileListMode } from '../config/fileListConfig';
import Menu from './ui/menu/Menu';
import MenuGroup from './ui/menu/MenuGroup';
import MenuItem from './ui/menu/MenuItem';
import { useUIStore } from '../store/useUIStore';
import { useAuthStore } from '../store/useAuthStore';

interface ContextMenuProps {
  x: number;
  y: number;
  onClose: () => void;
  mode: FileListMode;
}

export default function ContextMenu({ x, y, onClose, mode }: ContextMenuProps) {
  const files = useFileStore((state) => state.files);
  const selectedUuids = useFileStore((state) => state.selectedUuids);
  const currentUser = useAuthStore((state) => state.user);
  const openMoveFilesModal = useUIStore((state) => state.openMoveFilesModal);
  const openShareModal = useUIStore((state) => state.openShareModal);
  const {
    deleteFiles,
    restoreFiles,
    permanentlyDeleteFiles,
    downloadSelected,
    openRenameModal,
    addToFavorites,
    removeFromFavorites,
  } = useFileActions();

  const isMultiple = selectedUuids.length > 1;
  const selectedFiles = files.filter((f) => selectedUuids.includes(f.uuid));
  const allInFavorites = selectedFiles.length > 0 && selectedFiles.every((f) => f.isFavorite);

  const handleRename = () => {
    if (selectedUuids.length !== 1) return;
    const targetItem = files.find((f) => f.uuid === selectedUuids[0]);
    if (targetItem) openRenameModal(targetItem);
    onClose();
  };

  const handleFavoriteToggle = async () => {
    if (allInFavorites) {
      await removeFromFavorites(selectedUuids);
    } else {
      await addToFavorites(selectedUuids);
    }
    onClose();
  };

  const handlePermanentDelete = () => {
    if (window.confirm(`Удалить навсегда выбранные элементы (${selectedUuids.length})? Это действие нельзя отменить.`)) {
      void permanentlyDeleteFiles(selectedUuids);
    }
    onClose();
  };

  if (mode === 'trash') {
    return (
      <Menu x={x} y={y} onClose={onClose}>
        <MenuGroup showBorder={false}>
          <MenuItem
            icon={<RotateCcw size={18} />}
            label="Восстановить"
            onClick={() => { void restoreFiles(selectedUuids); onClose(); }}
          />
          <MenuItem
            icon={<Trash2 size={18} />}
            label="Удалить навсегда"
            danger
            onClick={handlePermanentDelete}
          />
        </MenuGroup>
      </Menu>
    );
  }

  return (
    <Menu x={x} y={y} onClose={onClose}>
      {/* Секция 1: Скачать & Переименовать */}
      <MenuGroup>
        <MenuItem
          icon={<Download size={18} />}
          label="Скачать"
          onClick={() => { downloadSelected(selectedUuids); onClose(); }}
        />
        <MenuItem
          icon={<Edit3 size={18} />}
          label="Переименовать"
          shortcut="Ctrl+Alt+E"
          disabled={isMultiple}
          onClick={() => { handleRename(); onClose(); }}
        />
      </MenuGroup>

      {/* Секция 2: Поделиться, Упорядочить, Сведения */}
      <MenuGroup>
        <MenuItem
          icon={<UserPlus size={18} />}
          label="Поделиться"
          disabled={isMultiple || selectedFiles.length !== 1 || selectedFiles[0]?.ownerUuid !== currentUser?.uuid}
          onClick={() => { openShareModal(selectedFiles[0]); onClose(); }}
        />
        <MenuItem
          icon={<Folder size={18} />}
          label="Упорядочить"
          submenu={
            <>
              <MenuItem
                icon={<FolderOutput size={18} />}
                label="Переместить"
                onClick={() => { openMoveFilesModal(selectedUuids); onClose(); }}
              />
              <MenuItem
                icon={
                  <Star 
                    size={16}
                    aria-hidden
                    className={allInFavorites ? 'shrink-0 text-secondary fill-current' : ''} 
                  />
                }
                label={allInFavorites ? 'Удалить из избранного' : 'Добавить в избранное'}
                onClick={handleFavoriteToggle}
              />
            </>
          }
        />
        <MenuItem
          icon={<Info size={18} />}
          label="Сведения о папке"
          disabled={isMultiple}
          onClick={() => { console.log('Сведения'); onClose(); }}
        />
      </MenuGroup>

      {/* Секция 3: Корзина */}
      <MenuGroup showBorder={false}>
        <MenuItem
          icon={<Trash2 size={18} />}
          label="Отправить в корзину"
          shortcut="Delete"
          danger
          onClick={() => { deleteFiles(selectedUuids); onClose(); }}
        />
      </MenuGroup>
    </Menu>
  );
}