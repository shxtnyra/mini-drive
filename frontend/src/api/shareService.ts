import { api } from './client';
import type {
  PublicNodeResponse,
  ShareDetails,
  SharePermission,
  ShareTargetsResponse,
} from '../types/api';

export const shareService = {
  async searchTargets(query: string): Promise<ShareTargetsResponse> {
    const response = await api.get<ShareTargetsResponse>('/search/share-targets', {
      params: { query },
    });
    return response.data;
  },

  async getShares(fileUuid: string): Promise<ShareDetails> {
    const response = await api.get<ShareDetails>(`/shares/${fileUuid}`);
    return response.data;
  },

  async addRecipients(
    fileUuid: string,
    userUuids: string[],
    groupUuids: string[],
    permission: SharePermission
  ): Promise<ShareDetails> {
    const response = await api.post<ShareDetails>('/shares', {
      fileUuid,
      userUuids,
      groupUuids,
      permission,
    });
    return response.data;
  },

  async setPublicLink(fileUuid: string, linkEnabled: boolean): Promise<ShareDetails> {
    const response = await api.post<ShareDetails>('/shares', {
      fileUuid,
      linkEnabled,
    });
    return response.data;
  },

  async revokeShare(shareUuid: string): Promise<void> {
    await api.delete(`/shares/${shareUuid}`);
  },

  async getPublicNode(token: string, nodeUuid?: string): Promise<PublicNodeResponse> {
    const path = nodeUuid
      ? `/public/shares/${token}/${nodeUuid}`
      : `/public/shares/${token}`;
    const response = await api.get<PublicNodeResponse>(path);
    return response.data;
  },

  async downloadPublicFile(token: string, fileUuid: string): Promise<Blob> {
    const response = await api.get(`/public/shares/${token}/${fileUuid}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },
};
