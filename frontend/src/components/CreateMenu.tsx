import { FolderPlus, FileUp, FolderUp } from 'lucide-react';
import { useCreateActions } from '../hooks/useCreateActions';
import Menu from './ui/menu/Menu';
import MenuItem from './ui/menu/MenuItem';
import MenuGroup from './ui/menu/MenuGroup';

interface CreateMenuProps {
  x: number;
  y: number;
  onClose: () => void;
}

export default function CreateMenu({ x, y, onClose }: CreateMenuProps) {
  const { openFilePicker, openFolderPicker, openCreateFolderModal } = useCreateActions();

  return (
    <Menu x={x} y={y} onClose={onClose}>
      <MenuGroup>
        <MenuItem
          icon={<FolderPlus size={19} />}
          label="Новая папка"
          shortcut="Alt+C, затем F"
          onClick={() => { openCreateFolderModal(); onClose(); }}
        />
      </MenuGroup>

      <MenuGroup showBorder={false}>
        <MenuItem
          icon={<FileUp size={19} />}
          label="Загрузить файл"
          shortcut="Alt+C, затем U"
          onClick={() => { openFilePicker(); onClose(); }}
        />
        <MenuItem
          icon={<FolderUp size={19} />}
          label="Загрузить папку"
          shortcut="Alt+C, затем I"
          onClick={() => { openFolderPicker(); onClose(); }}
        />
      </MenuGroup>
    </Menu>
  );
}