/** UserInfoResponse (/api/auth/me) */
export interface UserInfo {
  status: string;
  supabaseId: string;
  email: string | null;
  role: string;
}

export type DocumentType = "FILE" | "TEXT_REMINDER";


export interface DocumentItem {
  id: string;
  title: string;
  category: string;
  type: DocumentType;
  content: string | null;
  expiryDate: string | null;
  special: boolean;
  frequencyMonths: number | null;
  hasFile: boolean;
  createdBy: string;
  createdAt: string;
  updatedAt: string | null;
}


export interface DocumentVersion {
  id: string;
  content: string | null;
  expiryDate: string | null;
  hasFile: boolean;
  archivedAt: string;
}

export type AuditAction = "UPLOAD" | "RENEW" | "DELETE" | "PHYSICAL_DELETE";

export interface AuditLog {
  id: string;
  documentId: string;
  documentTitle: string | null;
  userId: string | null;
  userEmail: string | null;
  action: AuditAction;
  performedAt: string;
}

export interface Page<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
