import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { authService } from '../api/authService';
import type { RegisterRequest, UserDetail } from '../types/api';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserDetail | null;
  
  setTokens: (access: string, refresh: string) => void;
  setUser: (user: UserDetail) => void;
  login: (usernameOrEmail: string, password: string, rememberMe: boolean) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  loadUser: () => Promise<void>;
  isAuthenticated: () => boolean;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,

      setTokens: (access, refresh) => set({ accessToken: access, refreshToken: refresh }),
      setUser: (user) => set({ user }),

      clearAuth: () => set({ accessToken: null, refreshToken: null, user: null }),

      login: async (usernameOrEmail, password, _rememberMe = true) => {
        const tokens = await authService.login({ usernameOrEmail, password });
        set({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
        
        await get().loadUser();
      },

      register: async (data) => {
        const tokens = await authService.register(data);
        set({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
        await get().loadUser();
      },

      logout: async () => {
        const refreshToken = get().refreshToken;
        if (refreshToken) {
          try {
            await authService.logout(refreshToken);
          } catch (error) {
            console.error('Logout error:', error);
          }
        }
        get().clearAuth()
      },

      loadUser: async () => {
        try {
          const user = await authService.getCurrentUser();
          set({ user });
        } catch (error) {
          console.error('Failed to load user:', error);
          set({ accessToken: null, refreshToken: null, user: null });
        }
      },

      isAuthenticated: () => !!get().accessToken,
    }),
    {
      name: 'mini-drive-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ 
        accessToken: state.accessToken, 
        refreshToken: state.refreshToken,
        user: state.user 
      }),
    }
  )
);