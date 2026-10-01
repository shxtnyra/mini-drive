import { create } from 'zustand';
import { persist } from 'zustand/middleware';

type Theme = 'light' | 'dark';

interface ThemeState {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  toggleTheme: () => void;
  hydrateTheme: () => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set, get) => ({
      theme: 'dark',
      
      setTheme: (theme) => {
        set({ theme });
        document.documentElement.setAttribute('data-theme', theme);
      },
      
      toggleTheme: () => {
        const newTheme = get().theme === 'light' ? 'dark' : 'light';
        get().setTheme(newTheme);
      },

      hydrateTheme: () => {
        const stored = localStorage.getItem('theme-storage');
        if (stored) {
          const parsed = JSON.parse(stored);
          const theme = parsed.state?.theme || 'dark';
          document.documentElement.setAttribute('data-theme', theme);
          set({ theme });
        } else {
          document.documentElement.setAttribute('data-theme', 'dark');
        }
      }
    }),
    {
      name: 'theme-storage',
      partialize: (state) => ({ theme: state.theme }),
    }
  )
);