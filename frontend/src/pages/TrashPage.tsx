import Breadcrumbs from '../components/Breadcrumbs';
import Toolbar from '../components/Toolbar';
import FileList from '../components/FileList';
import { useNodeList } from '../hooks/useNodeList';
import { getColumnsByMode } from '../config/fileListConfig';

export default function TrashPage() {
  const { loadMore } = useNodeList({ context: 'TRASH', mode: 'trash' });

  return (
    <>
      <Breadcrumbs />
      <Toolbar mode="trash" />
      <FileList 
        mode="trash" 
        columns={getColumnsByMode('trash')} 
        onLoadMore={loadMore} 
      />
    </>
  );
}