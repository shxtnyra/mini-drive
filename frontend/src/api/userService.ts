// api/userService.ts
import type { Session, UserDetail } from '../types/api';
import { api } from './client';

export const userService = {
  async getCurrentUser(): Promise<UserDetail> {
    const response = await api.get<UserDetail>('/users/me');
    return response.data;
  },

  async updateProfile(displayName: string): Promise<UserDetail> {
    const response = await api.patch<UserDetail>('/users/me', { displayName });
    return response.data;
  },

  async changePassword(oldPassword: string, newPassword: string): Promise<void> {
    await api.put('/users/me/password', { oldPassword, newPassword });
  },

  async requestEmailChange(newEmail: string, currentPassword: string): Promise<void> {
    await api.post('/users/email/change', { newEmail, currentPassword });
  },

  // Подтверждение первичного email
  async verifyEmail(code: string) {
    const response = await api.post('/users/me/email:verify', { code });
    return response.data; // Возвращает новые токены
  },

  async resendEmailCode(): Promise<void> {
    await api.post('/users/me/email:resend-code');
  },

  // Подтверждение смены email
  async confirmEmailChange(code: string) {
    const response = await api.post('/users/email/change:verify', { code });
    return response.data; // Возвращает новые токены
  },

  async getSessions(): Promise<Session[]> {
    const response = await api.get<Session[]>('/users/me/sessions');
    return response.data;
  },

  async revokeAllSessions(): Promise<void> {
    await api.post('/users/me/sessions:revoke:all');
  }
};