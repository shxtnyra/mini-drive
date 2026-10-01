import Breadcrumbs from '../components/Breadcrumbs';
import Toolbar from '../components/Toolbar';
import FileList from '../components/FileList';
import { useNodeList } from '../hooks/useNodeList';
import { getColumnsByMode } from '../config/fileListConfig';

export default function SharedPage() {
  const { loadMore } = useNodeList({ 
    context: 'SHARED', 
    mode: 'shared' 
  });

  return (
    <>
      <Breadcrumbs />
      <Toolbar />
      <FileList 
        mode="shared" 
        columns={getColumnsByMode('shared')} 
        onLoadMore={loadMore} 
      />
    </>
  );
}