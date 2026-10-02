import { apiRequest } from "./api";

/** Chiede al backend un URL firmato e lo apre in una nuova scheda. */
export async function openSignedUrl(path: string): Promise<void> {
  const { url } = await apiRequest<{ url: string }>(path, { method: "POST" });
  window.open(url, "_blank");
}

export function downloadDocument(id: string): Promise<void> {
  return openSignedUrl(`/documents/${id}/download`);
}

export function downloadVersion(versionId: string): Promise<void> {
  return openSignedUrl(`/documents/versions/${versionId}/download`);
}

export async function deleteDocument(id: string): Promise<void> {
  await apiRequest<null>(`/documents/${id}`, { method: "DELETE" });
}
