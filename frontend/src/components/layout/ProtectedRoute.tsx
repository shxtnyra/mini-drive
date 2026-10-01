import { Navigate } from 'react-router-dom';
import { Loader } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useAuthStore } from '../../store/useAuthStore';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { accessToken, user, loadUser } = useAuthStore();
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const init = async () => {
      if (accessToken && !user) {
        await loadUser();
      }
      setIsLoading(false);
    };
    init();
  }, [accessToken, user, loadUser]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <Loader className="animate-spin text-blue-600" size={48} />
      </div>
    );
  }

  if (!accessToken) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}