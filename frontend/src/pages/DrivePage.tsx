  import Breadcrumbs from '../components/Breadcrumbs';
  import Toolbar from '../components/Toolbar';
  import FileList from '../components/FileList';
  import DropZone from '../components/DropZone';
  import { useNodeList } from '../hooks/useNodeList';
  import { getColumnsByMode } from '../config/fileListConfig';

  export default function DrivePage() {
    const { loadMore, refresh } = useNodeList({ context: 'DRIVE', mode: 'drive' });

    return (
      <DropZone>
        <Breadcrumbs />
        <Toolbar />
        <FileList 
          mode="drive" 
          columns={getColumnsByMode('drive')} 
          onLoadMore={loadMore} 
          onRefresh={refresh}
        />
      </DropZone>
    );
  }