import { api } from './client';
import type { Tokens, UserDetail } from '../types/api';

interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  displayName?: string;
}

export const authService = {
  async login(data: LoginRequest): Promise<Tokens> {
    const response = await api.post('/auth/login', data);
    return response.data;
  },

  async register(data: RegisterRequest): Promise<Tokens> {
    const response = await api.post('/auth/register', data);
    return response.data;
  },

  async logout(refreshToken: string): Promise<void> {
    await api.post('/auth/logout', { refreshToken });
  },

  async getCurrentUser(): Promise<UserDetail> {
    const response = await api.get('/users/me');
    return response.data;
  },

  // Подтверждение email
  async verifyEmail(code: string): Promise<Tokens> {
    const response = await api.post('/users/me/email:verify', { code });
    return response.data;
  },

  async resendEmailCode(): Promise<void> {
    await api.post('/users/me/email:resend-code');
  },
};