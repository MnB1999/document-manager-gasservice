import { createContext, useCallback, useContext, useState, type ReactNode } from "react";

type ToastType = "info" | "success" | "error";
type ShowToast = (message: string, type?: ToastType) => void;

interface ToastItem {
  id: number;
  message: string;
  type: ToastType;
}

const ToastContext = createContext<ShowToast | null>(null);

let nextToastId = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const showToast = useCallback<ShowToast>((message, type = "info") => {
    const id = nextToastId++;
    setToasts((list) => [...list, { id, message, type }]);
    setTimeout(() => setToasts((list) => list.filter((t) => t.id !== id)), 4000);
  }, []);

  return (
    <ToastContext.Provider value={showToast}>
      {children}
      <div className="toast-stack">
        {toasts.map((t) => (
          <div key={t.id} className={`toast ${t.type}`}>{t.message}</div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ShowToast {
  const showToast = useContext(ToastContext);
  if (!showToast) throw new Error("useToast va usato dentro <ToastProvider>");
  return showToast;
}
