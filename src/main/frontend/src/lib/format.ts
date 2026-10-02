import type { AuditAction } from "../types";

/** "YYYY-MM-DD" -> "DD/MM/YYYY" */
export function formatDate(isoDate: string | null): string {
  if (!isoDate) return "—";
  const [y, m, d] = isoDate.split("-");
  return `${d}/${m}/${y}`;
}

export function formatDateTime(isoDateTime: string | null): string {
  if (!isoDateTime) return "—";
  const date = new Date(isoDateTime);
  return date.toLocaleDateString("it-IT") + " " +
    date.toLocaleTimeString("it-IT", { hour: "2-digit", minute: "2-digit" });
}

export function expiryClass(isoDate: string): string {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const daysLeft = Math.round((new Date(isoDate).getTime() - today.getTime()) / 86400000);

  if (daysLeft < 0) return "expiry-expired";
  if (daysLeft <= 30) return "expiry-soon";
  return "expiry-ok";
}

export function daysSince(isoDateTime: string): number {
  return Math.floor((Date.now() - new Date(isoDateTime).getTime()) / 86400000);
}

const AUDIT_LABELS: Record<AuditAction, string> = {
  UPLOAD: "Primo caricamento",
  RENEW: "Rinnovo",
  DELETE: "Eliminazione",
  PHYSICAL_DELETE: "Eliminazione definitiva"
};

export function auditActionLabel(action: AuditAction): string {
  return AUDIT_LABELS[action];
}
