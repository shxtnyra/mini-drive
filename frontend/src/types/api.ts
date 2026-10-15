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

export interface DirectUploadRequest {
  fileName: string;
  mimeType?: string;
  parentUuid?: string | null;
}

export interface BeginUploadResponse {
  sessionUuid: string;
  chunkSize: number;
  totalParts: number;
}

export interface UploadStatusResponse {
  sessionUuid: string;
  uploadedParts: number[];
  totalParts: number;
}

export interface ShareUserTarget {
  uuid: string;
  displayName: string;
  username: string;
  email: string;
}

export interface ShareGroupTarget {
  uuid: string;
  name: string;
  membersCount: number;
}

export interface ShareTargetsResponse {
  users: ShareUserTarget[];
  groups: ShareGroupTarget[];
}

export type SharePermission = 'READ' | 'WRITE' | 'ALL';
export type ShareRecipientType = 'USER' | 'GROUP';

export interface ShareRecipient {
  shareUuid: string;
  type: ShareRecipientType;
  recipientUuid: string;
  name: string;
  email: string | null;
  permission: SharePermission;
}

export interface PublicLink {
  enabled: boolean;
  token: string | null;
  permission: SharePermission | null;
}

export interface ShareDetails {
  nodeUuid: string;
  recipients: ShareRecipient[];
  link: PublicLink;
}

export interface PublicNodeResponse {
  node: NodeResponse;
  children: NodeResponse[];
  permission: SharePermission;
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