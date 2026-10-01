import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { Loader, AlertCircle, Eye, EyeOff } from 'lucide-react';
import Logo from '../components/icon/Logo';
import Checkbox from '../components/ui/Checkbox';

export default function LoginPage() {
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);
  
  const [formData, setFormData] = useState({
    usernameOrEmail: '',
    password: '',
    rememberMe: true,
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setFormData({
      ...formData,
      [e.target.name]: value,
    });
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await login(formData.usernameOrEmail, formData.password, formData.rememberMe);
      navigate('/drive/my-drive');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Неверный логин или пароль');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-page flex">
      {/* Левая часть - Логотип с лучами */}
      <div className="hidden lg:flex lg:w-1/2 relative items-center justify-center overflow-hidden">
        {/* Фоновые тонкие линии */}
        <div className="absolute inset-0 opacity-20">
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full h-px bg-linear-to-r from-transparent via-secondary to-transparent"></div>
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-px h-full bg-linear-to-b from-transparent via-secondary to-transparent"></div>
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full h-px bg-linear-to-r from-transparent via-border to-transparent rotate-45"></div>
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-px h-full bg-linear-to-b from-transparent via-border to-transparent rotate-45"></div>
        </div>

        <div className="relative z-10">
          {/* Пульсирующие круги */}
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 border border-gray-800 rounded-full animate-ping" style={{ animationDuration: '3s' }}></div>
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-80 h-80 border border-gray-800 rounded-full animate-ping" style={{ animationDuration: '3s', animationDelay: '0.5s' }}></div>
          
          {/* Логотип */}
          <div className="relative flex items-center justify-center">
            <div className="w-32 h-32">
              <Logo color="var(--color-primary)" size={128} />
            </div>
          </div>
        </div>

        <div className="absolute top-8 left-8">
          <h1 className="text-primary text-lg font-semibold tracking-wide">Mini Drive</h1>
        </div>
      </div>

      {/* Правая часть - Форма входа */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 sm:p-12 relative">
        <Link 
          to="/register"
          className="absolute top-8 right-8 text-secondary hover:text-primary text-sm transition-colors"
        >
          Создать аккаунт
        </Link>

        <div className="w-full max-w-md">
          <h2 className="text-5xl font-light text-primary mb-12 tracking-tight">Вход</h2>

          {error && (
            <div className="mb-6 p-3 bg-error-bg border border-error-border rounded flex items-center gap-2">
              <AlertCircle color='var(--color-error-icon)' size={16} />
              <p className="text-sm text-error-text">{error}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="relative">
              <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">
                Email
              </label>
              <input
                type="text"
                name="usernameOrEmail"
                value={formData.usernameOrEmail}
                onChange={handleChange}
                required
                autoComplete="username"
                className="w-full bg-transparent border-b border-border text-primary p-3 focus:outline-none focus:border-focus transition-colors placeholder-placeholder"
                placeholder="mark.johnson@gmail.com"
              />
            </div>

            <div className="relative">
              <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">
                Пароль
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  required
                  autoComplete="current-password"
                  className="w-full bg-transparent border-b border-border text-primary p-3 pr-10 focus:outline-none focus:border-focus transition-colors placeholder-placeholder"
                  placeholder="••••••••"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-0 top-1/2 -translate-y-1/2 text-secondary hover:text-primary transition-colors"
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              <div className="mt-4">
                <Checkbox 
                  id="remember-me"
                  checked={formData.rememberMe}
                  onChange={(checked) => setFormData(prev => ({ ...prev, rememberMe: checked }))}
                  label="Запомнить меня"
                />
              </div>
            </div>

            <div className="flex justify-end">
              <button
                type="submit"
                disabled={isLoading}
                className="py-2.5 px-6 bg-btn text-btn-text font-medium hover:bg-btn-hover transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center group shadow-btn hover:shadow-btn-hover hover:scale-105 active:scale-95"
              >
                {isLoading ? (
                  <Loader className="animate-spin text-placeholder" size={24} />
                ) : (
                  <span className="text-sm font-semibold">ВОЙТИ</span>
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}