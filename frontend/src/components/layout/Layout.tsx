import { Outlet } from 'react-router-dom';
import { useEffect } from 'react';
import UploadQueue from '../UploadQueue';
import { useAuthStore } from '../../store/useAuthStore';
import Sidebar from './Sidebar';
import Header from './Header';
import EmailVerificationBanner from './EmailVerificationBanner';
import GlobalModals from '../modals/GlobalModals';

export default function Layout() {
  const user = useAuthStore((state) => state.user);
  const loadUser = useAuthStore((state) => state.loadUser);

  useEffect(() => {
    loadUser();
  }, []);

  return (
    <div className="flex h-screen bg-page text-primary overflow-hidden">
      <Sidebar/>
      <main className="flex-1 flex flex-col overflow-hidden">
        <Header/>
        {user && !user.emailVerified && <EmailVerificationBanner/>}

        <div className="flex-1 overflow-y-auto p-6 bg-page">
          <Outlet />
        </div>
      </main>
      <UploadQueue />
      <GlobalModals/>
    </div>
  );
}
