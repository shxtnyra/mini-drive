import Breadcrumbs from '../components/Breadcrumbs';
import Toolbar from '../components/Toolbar';
import FileList from '../components/FileList';
import { useNodeList } from '../hooks/useNodeList';
import { getColumnsByMode } from '../config/fileListConfig';

export default function FavoritePage() {
  const { loadMore } = useNodeList({ 
    context: 'FAVORITE', 
    mode: 'favorite' 
  });

  return (
    <>
      <Breadcrumbs />
      <Toolbar />
      <FileList 
        mode="favorite" 
        columns={getColumnsByMode('favorite')} 
        onLoadMore={loadMore} 
      />
    </>
  );
}