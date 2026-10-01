import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { Loader, AlertCircle, Eye, EyeOff } from 'lucide-react';
import Logo from '../components/icon/Logo';

export default function RegisterPage() {
  const navigate = useNavigate();
  const register = useAuthStore((state) => state.register);
  
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    displayName: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
    setError('');
  };

  const validateForm = (): boolean => {
    if (formData.password.length < 8) {
      setError('Пароль должен содержать минимум 8 символов');
      return false;
    }
    if (formData.password !== formData.confirmPassword) {
      setError('Пароли не совпадают');
      return false;
    }
    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!validateForm()) return;

    setIsLoading(true);
    try {
      await register({
        username: formData.username,
        email: formData.email,
        password: formData.password,
        displayName: formData.displayName || formData.username,
      });
      navigate('/drive/my-drive');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка регистрации');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-page flex">
      {/* Левая часть - Логотип с лучами */}
      <div className="hidden lg:flex lg:w-1/2 relative items-center justify-center overflow-hidden">
        {/* Фоновые тонкие линии (используем цвета границ и вторичный текст) */}
        <div className="absolute inset-0 opacity-20">
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full h-px bg-linear-to-r from-transparent via-secondary to-transparent"></div>
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-px h-full bg-linear-to-b from-transparent via-secondary to-transparent"></div>
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full h-px bg-linear-to-r from-transparent via-border to-transparent rotate-45"></div>
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-px h-full bg-linear-to-b from-transparent via-border to-transparent rotate-45"></div>
        </div>

        <div className="relative z-10">
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

      {/* Правая часть - Форма регистрации */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 sm:p-12 relative overflow-y-auto">
        <Link 
          to="/login"
          className="absolute top-8 right-8 text-secondary hover:text-primary text-sm transition-colors"
        >
          Уже есть аккаунт?
        </Link>

        <div className="w-full max-w-md mt-12">
          <h2 className="text-5xl font-light text-primary mb-3 tracking-tight">Регистрация</h2>
          <p className="text-subtle mb-10">Создайте аккаунт для доступа к хранилищу</p>

          {error && (
            <div className="mb-6 p-3 bg-error-bg border border-error-border rounded flex items-start gap-2 animate-in fade-in slide-in-from-top-2 duration-200">
              <AlertCircle color='var(--color-error-icon)' size={16} />
              <p className="text-sm text-error-text">{error}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="grid grid-cols-2 gap-6">
              <div className="relative">
                <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">
                  Имя пользователя *
                </label>
                <input
                  type="text"
                  name="username"
                  value={formData.username}
                  onChange={handleChange}
                  required
                  minLength={3}
                  maxLength={50}
                  autoComplete="username"
                  className="w-full bg-transparent border-b border-border text-primary py-3 focus:outline-none focus:border-border-focus transition-colors duration-200 placeholder-placeholder"
                  placeholder="johndoe"
                />
              </div>

              <div className="relative">
                <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">
                  Отображаемое имя
                </label>
                <input
                  type="text"
                  name="displayName"
                  value={formData.displayName}
                  onChange={handleChange}
                  autoComplete="name"
                  className="w-full bg-transparent border-b border-border text-primary py-3 focus:outline-none focus:border-border-focus transition-colors duration-200 placeholder-placeholder"
                  placeholder="John Doe"
                />
              </div>
            </div>

            <div className="relative">
              <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">
                Email *
              </label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                required
                autoComplete="email"
                className="w-full bg-transparent border-b border-border text-primary py-3 focus:outline-none focus:border-border-focus transition-colors duration-200 placeholder-placeholder"
                placeholder="you@example.com"
              />
            </div>

            <div className="relative">
              <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">
                Пароль *
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  required
                  minLength={8}
                  maxLength={128}
                  autoComplete="new-password"
                  className="w-full bg-transparent border-b border-border text-primary py-3 pr-10 focus:outline-none focus:border-border-focus transition-colors duration-200 placeholder-placeholder"
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
              <p className="text-xs text-subtle mt-1">Минимум 8 символов</p>
            </div>

            <div className="relative">
              <label className="block text-secondary text-xs mb-2 uppercase tracking-wider">
                Подтвердите пароль *
              </label>
              <div className="relative">
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  name="confirmPassword"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  required
                  autoComplete="new-password"
                  className="w-full bg-transparent border-b border-border text-primary py-3 pr-10 focus:outline-none focus:border-border-focus transition-colors duration-200 placeholder-placeholder"
                  placeholder="••••••••"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-0 top-1/2 -translate-y-1/2 text-secondary hover:text-primary transition-colors"
                >
                  {showConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <div className="flex justify-end pt-4">
              <button
                type="submit"
                disabled={isLoading}
                className="py-2.5 px-6 bg-btn text-btn-text font-medium hover:bg-btn-hover transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center group shadow-lg hover:shadow-(--color-shadow-btn-hover) hover:scale-105 active:scale-95"
              >
                {isLoading ? (
                  <Loader className="animate-spin text-primary" size={24} />
                ) : (
                  <span className="text-sm font-semibold">СОЗДАТЬ</span>
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}