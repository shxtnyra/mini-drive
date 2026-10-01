import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/layout/Layout';
import DrivePage from './pages/DrivePage';
import TrashPage from './pages/TrashPage'; // Импортируем нашу новую страницу
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import { useAuthStore } from './store/useAuthStore';
import { useEffect } from 'react';
import ProfilePage from './pages/ProfilePage';
import FavoritePage from './pages/FavoritePage';
import SharedPage from './pages/SharedPage';
import ProtectedRoute from './components/layout/ProtectedRoute';

function App() {
  const { accessToken, user, loadUser } = useAuthStore();

  useEffect(() => {
    if (accessToken && !user) {
      loadUser();
    }
  }, [accessToken, user, loadUser]);

  return (
    <BrowserRouter>
      <Routes>
        {/* Публичные роуты */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Защищенные роуты */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          {/* Перенаправление с корня на Мой диск */}
          <Route index element={<Navigate to="/drive/my-drive" replace />} />
          
          {/* Мой диск и вложенные папки */}
          <Route path="/drive/my-drive" element={<DrivePage />} />
          <Route path="/drive/folders/:folderId" element={<DrivePage />} />

          {/* Остальные разделы */}
          <Route path="/drive/shared" element={<SharedPage />} />
          <Route path="/drive/favorite" element={<FavoritePage />} />
          <Route path="/drive/trash" element={<TrashPage />} />
          
          <Route path="/drive/profile" element={<ProfilePage />} />

          {/* Перенаправление на главную для неизвестных роутов внутри защищенной зоны */}
          <Route path="*" element={<Navigate to="/drive/my-drive" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;