import { useCurrentUser } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { deleteDocument, downloadDocument } from "../lib/documentActions";
import { expiryClass, formatDate, formatDateTime } from "../lib/format";
import type { DocumentItem } from "../types";


const DELETE_CONFIRM_MESSAGE = "Vuoi davvero eliminare questo documento? Non sarà più visibile nell'archivio.";

export interface DocumentRowHandlers {
  onDetail: (id: string) => void;
  onRenew: (id: string) => void;
  onChanged: () => void; // dopo eliminazione: ricarica la vista
}

export function DocumentsTable({ docs, handlers }: { docs: DocumentItem[]; handlers: DocumentRowHandlers }) {
  const user = useCurrentUser();
  const showToast = useToast();

  if (docs.length === 0) {
    return <div className="empty-state">Nessun documento trovato.</div>;
  }

  async function handleDownload(id: string) {
    try {
      await downloadDocument(id);
    } catch (err) {
      showToast((err as Error).message, "error");
    }
  }

  async function handleDelete(id: string) {
    if (!confirm(DELETE_CONFIRM_MESSAGE)) return;
    try {
      await deleteDocument(id);
      showToast("Documento eliminato.", "success");
      handlers.onChanged();
    } catch (err) {
      showToast((err as Error).message, "error");
    }
  }

  return (
    <table>
      <thead>
        <tr><th>Titolo</th><th>Categoria</th><th>Tipo</th><th>Scadenza</th><th>Creato il</th><th>Azioni</th></tr>
      </thead>
      <tbody>
        {docs.map((doc) => (
          <tr key={doc.id}>
            <td>{doc.title} {doc.special && <span className="badge-special">riservato</span>}</td>
            <td>{doc.category}</td>
            <td>{doc.type === "FILE" ? "File" : "Promemoria"}</td>
            <td><ExpiryBadge date={doc.expiryDate} /></td>
            <td>{formatDateTime(doc.createdAt)}</td>
            <td className="row-actions">
              <button onClick={() => handlers.onDetail(doc.id)}>Dettagli</button>
              {doc.hasFile && <button onClick={() => handleDownload(doc.id)}>Scarica</button>}
              <button onClick={() => handlers.onRenew(doc.id)}>Rinnova</button>
              {user.isAdmin && <button className="danger" onClick={() => handleDelete(doc.id)}>Elimina</button>}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function ExpiryBadge({ date }: { date: string | null }) {
  if (!date) return <>—</>;
  return <span className={`expiry-badge ${expiryClass(date)}`}>{formatDate(date)}</span>;
}
