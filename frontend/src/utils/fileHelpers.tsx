import { 
  Folder, FileText, FileImage, FileVideo, FileAudio, 
  FileArchive, FileCode, FileSpreadsheet 
} from 'lucide-react';
import type { NodeResponse } from '../types/api';

export function getFileIcon(file: NodeResponse) {
  if (file.fileType === 'FOLDER') {
    return <Folder className="text-icon-folder" size={20} />;
  }

  const mime = file.mimeType?.toLowerCase() || '';
  const name = file.filename.toLowerCase();

  if (mime.startsWith('image/')) return <FileImage className="text-icon-image" size={20} />;
  if (mime.startsWith('video/')) return <FileVideo className="text-icon-video" size={20} />;
  if (mime.startsWith('audio/')) return <FileAudio className="text-icon-audio" size={20} />;
  if (mime.includes('pdf')) return <FileText className="text-icon-video" size={20} />;
  
  if (mime.includes('spreadsheet') || mime.includes('excel') || name.endsWith('.csv') || name.endsWith('.xlsx')) {
    return <FileSpreadsheet className="text-icon-doc" size={20} />;
  }
  
  if (mime.includes('zip') || mime.includes('rar') || mime.includes('tar') || mime.includes('x-zip')) {
    return <FileArchive className="text-icon-archive" size={20} />;
  }
  
  if (mime.includes('javascript') || mime.includes('typescript') || mime.includes('json') || name.endsWith('.js') || name.endsWith('.ts') || name.endsWith('.py')) {
    return <FileCode className="text-icon-code" size={20} />;
  }

  return <FileText className="text-icon-default" size={20} />;
}

export const FileNameCell = ({ file }: { file: NodeResponse }) => (
  <div className="flex items-center gap-3 min-w-0"> 
    {getFileIcon(file)}
    <span 
      className="font-medium text-primary truncate" 
      title={file.filename}
    >
      {file.filename}
    </span>
  </div>
);