import { useState, type SubmitEvent } from "react";
import { Modal, ModalHeader } from "../components/Modal";
import { Loaded } from "../components/StatusViews";
import { useToast } from "../context/ToastContext";
import { apiRequest } from "../lib/api";
import { useLoad } from "../lib/useLoad";
import type { DocumentItem } from "../types";

export function RenewModal({ id, onClose, onSaved }: { id: string; onClose: () => void; onSaved: () => void }) {
  const { state, retry } = useLoad(() => apiRequest<DocumentItem>(`/documents/${id}`), [id]);

  return (
    <Modal onClose={onClose}>
      <Loaded state={state} onRetry={retry}>
        {(doc) => <RenewForm doc={doc} onClose={onClose} onSaved={onSaved} />}
      </Loaded>
    </Modal>
  );
}

function RenewForm({ doc, onClose, onSaved }: { doc: DocumentItem; onClose: () => void; onSaved: () => void }) {
  const showToast = useToast();
  const [expiryDate, setExpiryDate] = useState(doc.expiryDate ?? "");
  const [file, setFile] = useState<File | null>(null);
  const [content, setContent] = useState(doc.content ?? "");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);

    const form = new FormData();
    form.append("expiryDate", expiryDate);
    if (doc.type === "FILE") {
      if (file) form.append("file", file);
    } else {
      form.append("content", content);
    }

    setSubmitting(true);
    try {
      await apiRequest<DocumentItem>(`/documents/renew/${doc.id}`, { method: "PUT", form });
      showToast("Documento rinnovato.", "success");
      onSaved();
      onClose();
    } catch (err) {
      setError((err as Error).message);
      setSubmitting(false);
    }
  }

  return (
    <>
      <ModalHeader title={`Rinnova "${doc.title}"`} onClose={onClose} />
      <form onSubmit={handleSubmit}>
        <label className="field">
          <span>Nuova data di scadenza</span>
          <input type="date" required value={expiryDate} onChange={(e) => setExpiryDate(e.target.value)} />
        </label>

        {doc.type === "FILE" ? (
          <label className="field">
            <span>Nuovo file (opzionale, sostituisce l'attuale)</span>
            <input type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
          </label>
        ) : (
          <label className="field">
            <span>Nuovo testo</span>
            <textarea rows={4} value={content} onChange={(e) => setContent(e.target.value)} />
          </label>
        )}

        {error && <p className="form-error">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>Annulla</button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? "Rinnovo" : "Conferma rinnovo"}
          </button>
        </div>
      </form>
    </>
  );
}
