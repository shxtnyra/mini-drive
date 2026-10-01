import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { useThemeStore } from './hooks/useThemeStore.ts';

useThemeStore.getState().hydrateTheme();

createRoot(document.getElementById('root')!).render(
  <App />
)
