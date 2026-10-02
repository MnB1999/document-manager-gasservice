import { CONFIG } from "../config";
import { getAccessToken } from "./auth";

type QueryValue = string | number | null;

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  query?: Record<string, QueryValue>;
  form?: FormData;
}


export async function apiRequest<T>(path: string, { method = "GET", query, form }: RequestOptions = {}): Promise<T> {
  const token = await getAccessToken();

  let url = CONFIG.API_BASE_URL + path;
  if (query) {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== null && value !== "") {
        params.append(key, String(value));
      }
    }
    const qs = params.toString();
    if (qs) url += "?" + qs;
  }

  const response = await fetch(url, {
    method,
    headers: { Authorization: "Bearer " + token },
    body: form
  });

  if (response.status === 204) {
    return null as T;
  }

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const message = data && data.message ? data.message : `Errore ${response.status}`;
    throw new Error(message);
  }

  return data as T;
}
