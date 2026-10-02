import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { apiRequest } from "../lib/api";
import { logout, supabase } from "../lib/auth";
import type { UserInfo } from "../types";
import { useToast } from "./ToastContext";

export interface CurrentUser extends UserInfo {
  isAdmin: boolean;
}

type AuthState =
  | { status: "loading" }
  | { status: "loggedOut" }
  | { status: "loggedIn"; user: CurrentUser };

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const showToast = useToast();
  const [supabaseUserId, setSupabaseUserId] = useState<string | null | undefined>(undefined);
  const [state, setState] = useState<AuthState>({ status: "loading" });

  useEffect(() => {
    const { data } = supabase.auth.onAuthStateChange((_event, session) => {
      setSupabaseUserId(session ? session.user.id : null);
    });
    return () => data.subscription.unsubscribe();
  }, []);

  useEffect(() => {
    if (supabaseUserId === undefined) return;
    if (supabaseUserId === null) {
      setState({ status: "loggedOut" });
      return;
    }

    let cancelled = false;
    setState({ status: "loading" });
    apiRequest<UserInfo>("/auth/me").then(
      (user) => {
        if (cancelled) return;
        setState({ status: "loggedIn", user: { ...user, isAdmin: user.role.toUpperCase() === "ADMIN" } });
      },
      (err: Error) => {
        if (cancelled) return;
        showToast(err.message, "error");
        logout();
      }
    );
    return () => { cancelled = true; };
  }, [supabaseUserId, showToast]);

  return <AuthContext.Provider value={state}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const state = useContext(AuthContext);
  if (!state) throw new Error("useAuth va usato dentro <AuthProvider>");
  return state;
}

export function useCurrentUser(): CurrentUser {
  const state = useAuth();
  if (state.status !== "loggedIn") throw new Error("useCurrentUser chiamato senza utente autenticato");
  return state.user;
}
