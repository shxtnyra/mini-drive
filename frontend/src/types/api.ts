export interface Tokens {
  accessToken: string;
  refreshToken: string;
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  displayName?: string;
}

// Типы для файлов и папок (Node)
export interface NodeResponse {
  uuid: string;
  fileType: 'FILE' | 'FOLDER';
  filename: string;
  ownerUuid: string;
  parentUuid?: string;
  mimeType?: string;
  sizeBytes: number;
  checksumSha256?: string;
  isFavorite: boolean;
  isShared: boolean;
  createdAt: string;
  updatedAt: string;
  deletedAt?: string;
}

export interface BreadcrumbItem {
  uuid: string | null,
  name: string
}

export interface CursorDto {
  uuid: string;
  value: string;
}

export interface CursorResponse<T> {
  items: T[];
  nextCursor: CursorDto | null;
  hasNext: boolean;
  pageSize: number;
}

export interface FolderResponse {
  breadcrumbs: BreadcrumbItem[],
  files: CursorResponse<NodeResponse>,
}

export interface BeginUploadRequest {
  fileName: string;
  fileSize: number;
  mimeType?: string;
  parentUuid?: string | null;
  requestedChunkSize?: number;
}

export interface BeginUploadResponse extends BeginUploadRequest {
  sessionUuid: string;
  chunkSize: number;
  totalParts: number;
}

export interface UserDetail {
  uuid: string;
  username: string;
  email: string;
  emailVerified: boolean;
  displayName: string;
  quotaBytes: number;
  usedBytes: number;
  isAdmin: boolean;
  createdAt: string;
}

export interface Session {
  refreshToken: string;
  ipAddress: string;
  userAgent: string;
  deviceName: string;
  lastUsedAt: string;
}