import type { ReactNode } from "react";
import type { LoadState } from "../lib/useLoad";

export function Loading() {
  return (
    <div className="loading-state">
      <div className="spinner"></div>
      <span>Caricamento in corso...</span>
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="error-state">
      <span>{message}</span>
      <button className="btn btn-secondary" onClick={onRetry}>Riprova</button>
    </div>
  );
}

export function Loaded<T>({ state, onRetry, children }: {
  state: LoadState<T>;
  onRetry: () => void;
  children: (data: T) => ReactNode;
}) {
  if (state.status === "loading") return <Loading />;
  if (state.status === "error") return <ErrorState message={state.message} onRetry={onRetry} />;
  return <>{children(state.data)}</>;
}
