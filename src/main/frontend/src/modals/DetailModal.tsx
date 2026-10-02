import { CONFIG } from "../config";
import { Modal, ModalHeader } from "../components/Modal";
import { Loaded } from "../components/StatusViews";
import { useCurrentUser } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { apiRequest } from "../lib/api";
import { downloadVersion } from "../lib/documentActions";
import { auditActionLabel, daysSince, formatDate, formatDateTime } from "../lib/format";
import { useLoad } from "../lib/useLoad";
import type { AuditLog, DocumentItem, DocumentVersion } from "../types";

export function DetailModal({ id, onClose }: { id: string; onClose: () => void }) {
  const user = useCurrentUser();

  const { state, retry } = useLoad(
    () => Promise.all([
      apiRequest<DocumentItem>(`/documents/${id}`),
      apiRequest<DocumentVersion[]>(`/documents/${id}/history`),
      user.isAdmin ? apiRequest<AuditLog[]>(`/audit/document/${id}`) : Promise.resolve<AuditLog[]>([])
    ]),
    [id, user.isAdmin]
  );

  return (
    <Modal onClose={onClose}>
      <Loaded state={state} onRetry={retry}>
        {([doc, history, auditLog]) => (
          <>
            <ModalHeader title={doc.title} onClose={onClose} />
            <div className="detail-section">
              <dl className="detail-grid">
                <dt>Categoria</dt><dd>{doc.category}</dd>
                <dt>Tipo</dt><dd>{doc.type === "FILE" ? "File" : "Promemoria testuale"}</dd>
                <dt>Scadenza</dt><dd>{formatDate(doc.expiryDate)}</dd>
                <dt>Rinnovo automatico</dt><dd>{doc.frequencyMonths ? `Ogni ${doc.frequencyMonths} mesi` : "No"}</dd>
                <dt>Riservato</dt><dd>{doc.special ? "Sì" : "No"}</dd>
                <dt>Creato il</dt><dd>{formatDateTime(doc.createdAt)}</dd>
                <dt>Ultima modifica</dt><dd>{formatDateTime(doc.updatedAt)}</dd>
              </dl>
              {doc.content && <p style={{ marginTop: 12, whiteSpace: "pre-wrap" }}>{doc.content}</p>}
            </div>

            <div className="detail-section">
              <h3>Versioni precedenti</h3>
              {history.length === 0 ? (
                <p>Nessuna versione precedente.</p>
              ) : (
                <ul className="version-list">
                  {history.map((v) => <VersionRow key={v.id} version={v} />)}
                </ul>
              )}
            </div>

            {user.isAdmin && (
              <div className="detail-section">
                <h3>Registro operazioni</h3>
                {auditLog.length === 0 ? (
                  <p>Nessuna operazione registrata.</p>
                ) : (
                  <ul className="audit-list">
                    {auditLog.map((l) => (
                      <li key={l.id}><span>{auditActionLabel(l.action)}</span><span>{formatDateTime(l.performedAt)}</span></li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </>
        )}
      </Loaded>
    </Modal>
  );
}

function VersionRow({ version }: { version: DocumentVersion }) {
  const showToast = useToast();
  const downloadable = version.hasFile && daysSince(version.archivedAt) <= CONFIG.VERSION_DOWNLOAD_DAYS;

  async function handleDownload() {
    try {
      await downloadVersion(version.id);
    } catch (err) {
      showToast((err as Error).message, "error");
    }
  }

  return (
    <li>
      <span>{formatDateTime(version.archivedAt)} — scadenza {formatDate(version.expiryDate)}</span>
      <span>
        {downloadable
          ? <button onClick={handleDownload}>Scarica</button>
          : (version.hasFile ? "File" : "Testo")}
      </span>
    </li>
  );
}
