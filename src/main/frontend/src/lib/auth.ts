import { createClient } from "@supabase/supabase-js";
import { CONFIG } from "../config";

export const supabase = createClient(CONFIG.SUPABASE_URL, CONFIG.SUPABASE_ANON_KEY);

export async function login(email: string, password: string): Promise<void> {
  const { error } = await supabase.auth.signInWithPassword({ email, password });
  if (error) {
    throw new Error(translateLoginError(error.message));
  }
}

export async function logout(): Promise<void> {
  await supabase.auth.signOut();
}

export async function getAccessToken(): Promise<string> {
  const { data } = await supabase.auth.getSession();
  if (!data.session) {
    throw new Error("Sessione scaduta. Effettua di nuovo il login.");
  }
  return data.session.access_token;
}

function translateLoginError(message: string): string {
  if (message.includes("Invalid login credentials")) {
    return "Email o password non corrette.";
  }
  return message;
}
