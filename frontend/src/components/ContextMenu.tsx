import { Download, Edit3, UserPlus, Folder, Info, Trash2, Star, FolderOutput } from 'lucide-react';
import { useFileStore } from '../store/useFileStore';
import { useFileActions } from '../hooks/useFileActions';
import Menu from './ui/menu/Menu';
import MenuGroup from './ui/menu/MenuGroup';
import MenuItem from './ui/menu/MenuItem';

interface ContextMenuProps {
  x: number;
  y: number;
  onClose: () => void;
}

export default function ContextMenu({ x, y, onClose }: ContextMenuProps) {
  const files = useFileStore((state) => state.files);
  const selectedUuids = useFileStore((state) => state.selectedUuids);
  const { deleteFiles, downloadSelected, openRenameModal, addToFavorites, removeFromFavorites } = useFileActions();

  const isMultiple = selectedUuids.length > 1;
  const selectedFiles = files.filter((f) => selectedUuids.includes(f.uuid));
  const allInFavorites = selectedFiles.length > 0 && selectedFiles.every((f) => (f as any).isFavorite);

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
          onClick={() => { console.log('Поделиться'); onClose(); }}
        />
        <MenuItem
          icon={<Folder size={18} />}
          label="Упорядочить"
          submenu={
            <>
              <MenuItem
                icon={<FolderOutput size={18} />}
                label="Переместить"
                onClick={() => { console.log('Переместить'); onClose(); }}
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