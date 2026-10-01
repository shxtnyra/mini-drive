import { AlertCircle } from "lucide-react";
import { userService } from "../../api/userService";
import { useUIStore } from "../../store/useUIStore";

export default function EmailVerificationBanner() {
  return (
    <div className="bg-error-bg border-b border-error-border px-6 py-3 flex items-center justify-between shrink-0">
      <div className="flex items-center gap-3 text-error-text">
        <AlertCircle size={20} className="text-error-icon shrink-0" />
        <span className="text-sm font-medium">
          Ваш email не подтвержден. Некоторые функции могут быть ограничены.
        </span>
      </div>
      
      <div className="flex items-center gap-4">
        <button 
          onClick={() => useUIStore.getState().openVerifyEmailModal('initial')}
          className="text-sm font-semibold text-error-text hover:text-primary underline underline-offset-2 transition-colors cursor-pointer"
        >
          Подтвердить сейчас
        </button>
        <button 
          onClick={async () => {
            await userService.resendEmailCode();
            alert('Новый код отправлен на ваш email');
          }}
          className="text-sm text-error-text/80 hover:text-primary transition-colors cursor-pointer"
        >
          Отправить код повторно
        </button>
      </div>
    </div>
  );
}