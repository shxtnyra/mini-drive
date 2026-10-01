import { ChevronRight, Grid3x3, Home, List, Plus } from 'lucide-react';
import { useFileStore } from '../store/useFileStore';
import { Link } from 'react-router-dom';
import { useRef, useState } from 'react';
import CreateMenu from './CreateMenu';

export default function Breadcrumbs() {
  const breadcrumbs = useFileStore((state) => state.breadcrumbs);
  const viewMode = useFileStore((state) => state.viewMode);
  const setViewMode = useFileStore((state) => state.setViewMode);

  const [isCreateMenuOpen, setIsCreateMenuOpen] = useState(false);
  const [menuPosition, setMenuPosition] = useState({ x: 0, y: 0 });
  const createButtonRef = useRef<HTMLButtonElement>(null);


  const handleCreateClick = () => {
    if (!createButtonRef.current) return;
    
    const rect = createButtonRef.current.getBoundingClientRect();
    
    setMenuPosition({
      x: rect.right - 320,
      y: rect.bottom + 4,
    });
    
    setIsCreateMenuOpen(true);
  };
  
  return (
    <nav className="flex items-center gap-1 mb-4 overflow-x-auto pb-1">
      <Link
        to="/drive/my-drive"
        className="flex items-center gap-1.5 px-2 py-1.5 hover:bg-sidebar-hover rounded-md transition-colors cursor-pointer shrink-0 group"
        title="Мой диск"
      >
        <Home size={20} className="text-secondary group-hover:text-primary transition-colors" />
        <span className="font-semibold text-lg text-primary">Мой диск</span>
      </Link>

      {breadcrumbs.map((crumb, index) => {
        const isLast = index === breadcrumbs.length - 1;
        const folderPath = `/drive/folders/${crumb.uuid}`;

        return (
          <div key={crumb.uuid} className="flex items-center gap-1 shrink-0">
            <ChevronRight size={16} className="text-subtle shrink-0" />
            
            {isLast ? (
              <span 
                className="px-2 py-1.5 text-lg font-bold text-primary truncate max-w-50"
                title={crumb.name}
              >
                {crumb.name}
              </span>
            ) : (
              <Link
                to={folderPath}
                className="px-2 py-1.5 hover:bg-sidebar-hover rounded-md transition-colors text-lg font-medium text-secondary hover:text-primary cursor-pointer truncate max-w-50"
                title={crumb.name}
              >
                {crumb.name}
              </Link>
            )}
          </div>
        );
      })}

      <div className="ml-auto flex items-center gap-1 shrink-0 pl-3 border-l border-border">
        <button
          ref={createButtonRef}
          onClick={handleCreateClick}
          className="p-2 rounded-md transition-colors hover:bg-sidebar-hover text-secondary hover:text-primary cursor-pointer"
          title="Создать"
        >
          <Plus size={20} />
        </button>
        <button
          onClick={() => setViewMode('list')}
          className={`p-2 rounded-md transition-colors ${
            viewMode === 'list'
              ? 'bg-sidebar-active text-primary'
              : 'hover:bg-sidebar-hover text-secondary hover:text-primary'
          }`}
          title="Список"
        >
          <List size={20} />
        </button>
        <button
          onClick={() => setViewMode('grid')}
          className={`p-2 rounded-md transition-colors ${
            viewMode === 'grid'
              ? 'bg-sidebar-active text-primary'
              : 'hover:bg-sidebar-hover text-secondary hover:text-primary'
          }`}
          title="Сетка"
        >
          <Grid3x3 size={20} />
        </button>
      </div>

      {/* Меню создания */}
      {isCreateMenuOpen && (
        <CreateMenu
          x={menuPosition.x}
          y={menuPosition.y}
          onClose={() => setIsCreateMenuOpen(false)}
        />
      )}
    </nav>
  );
}