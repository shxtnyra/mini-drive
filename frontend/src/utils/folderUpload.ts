import { fileService } from '../api/fileService';
import { useFileStore } from '../store/useFileStore';

interface UploadOptions {
  parentUuid?: string | null;
}

type UploadFile = (file: File, options: UploadOptions) => Promise<void>;

export async function uploadFolderStructure(
  files: File[],
  parentFolderUuid: string | null,
  uploadFile: UploadFile
): Promise<void> {
  const folderPaths = new Set<string>();
  const directoryByFile = new Map<File, string>();

  for (const file of files) {
    const pathParts = file.webkitRelativePath.split('/').filter(Boolean);
    if (pathParts.some((part) => part === '.' || part === '..')) {
      throw new Error(`Некорректный путь файла: ${file.name}`);
    }

    const directoryPath = pathParts.length > 1
      ? pathParts.slice(0, -1).join('/')
      : '';
    directoryByFile.set(file, directoryPath);

    const folders = pathParts.slice(0, -1);
    for (let index = 0; index < folders.length; index++) {
      folderPaths.add(folders.slice(0, index + 1).join('/'));
    }
  }

  const folderUuids = new Map<string, string>();
  const orderedFolderPaths = [...folderPaths].sort((first, second) => {
    const depthDifference = first.split('/').length - second.split('/').length;
    return depthDifference || first.localeCompare(second);
  });

  for (const folderPath of orderedFolderPaths) {
    const pathParts = folderPath.split('/');
    const folderName = pathParts[pathParts.length - 1];
    const parentPath = pathParts.slice(0, -1).join('/');
    const folderParentUuid = parentPath
      ? folderUuids.get(parentPath)
      : parentFolderUuid;

    if (folderParentUuid === undefined) {
      throw new Error(`Не удалось определить родительскую папку: ${folderPath}`);
    }

    const createdFolder = await fileService.createFolder(folderName, folderParentUuid);
    folderUuids.set(folderPath, createdFolder.uuid);

    // В списке открыт только один уровень, поэтому добавляем туда только его папки.
    if ((createdFolder.parentUuid ?? null) === useFileStore.getState().currentFolder) {
      useFileStore.getState().addFile(createdFolder);
    }
  }

  for (const file of files) {
    const directoryPath = directoryByFile.get(file) ?? '';
    const fileParentUuid = directoryPath
      ? folderUuids.get(directoryPath)
      : parentFolderUuid;

    if (fileParentUuid === undefined) {
      throw new Error(`Не удалось определить папку для файла: ${file.name}`);
    }

    void uploadFile(file, { parentUuid: fileParentUuid });
  }
}
