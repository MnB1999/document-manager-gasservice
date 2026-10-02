import { useState, type SubmitEvent } from "react";
import { CONFIG } from "../config";
import { Modal, ModalHeader } from "../components/Modal";
import { useCurrentUser } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { apiRequest } from "../lib/api";
import type { DocumentItem, DocumentType } from "../types";

const CUSTOM_CATEGORY = "__custom";

export function UploadModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const user = useCurrentUser();
  const showToast = useToast();

  const [title, setTitle] = useState("");
  const [category, setCategory] = useState<string>(CONFIG.CATEGORIES[0]);
  const [customCategory, setCustomCategory] = useState("");
  const [type, setType] = useState<DocumentType>("FILE");
  const [file, setFile] = useState<File | null>(null);
  const [content, setContent] = useState("");
  const [frequency, setFrequency] = useState("");
  const [expiryDate, setExpiryDate] = useState("");
  const [special, setSpecial] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);

    const form = new FormData();
    form.append("title", title.trim());
    form.append("category", category === CUSTOM_CATEGORY ? customCategory.trim() : category);
    form.append("expiryDate", expiryDate);
    form.append("isSpecial", user.isAdmin && special ? "true" : "false");

    if (type === "FILE") {
      if (file) form.append("file", file);
    } else {
      form.append("content", content);
      if (frequency) form.append("frequencyMonths", frequency);
    }

    setSubmitting(true);
    try {
      await apiRequest<DocumentItem>("/documents/upload", { method: "POST", form });
      showToast("Documento caricato.", "success");
      onSaved();
      onClose();
    } catch (err) {
      setError((err as Error).message);
      setSubmitting(false);
    }
  }

  return (
    <Modal onClose={onClose}>
      <ModalHeader title="Nuovo documento" onClose={onClose} />
      <form onSubmit={handleSubmit}>
        <label className="field">
          <span>Titolo</span>
          <input type="text" required value={title} onChange={(e) => setTitle(e.target.value)} />
        </label>

        <label className="field">
          <span>Categoria</span>
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            {CONFIG.CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
            <option value={CUSTOM_CATEGORY}>Altra categoria...</option>
          </select>
        </label>
        {category === CUSTOM_CATEGORY && (
          <input type="text" placeholder="Nome categoria" required
            value={customCategory} onChange={(e) => setCustomCategory(e.target.value)} />
        )}

        <div className="radio-group">
          <label>
            <input type="radio" name="up-type" checked={type === "FILE"} onChange={() => setType("FILE")} /> File
          </label>
          <label>
            <input type="radio" name="up-type" checked={type === "TEXT_REMINDER"} onChange={() => setType("TEXT_REMINDER")} /> Promemoria testuale
          </label>
        </div>

        {type === "FILE" ? (
          <label className="field">
            <span>File</span>
            <input type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
          </label>
        ) : (
          <>
            <label className="field">
              <span>Testo del promemoria</span>
              <textarea rows={4} value={content} onChange={(e) => setContent(e.target.value)} />
            </label>
            <label className="field">
              <span>Mesi di frequenza del promemoria</span>
              <input type="number" min={1} step={1} value={frequency} onChange={(e) => setFrequency(e.target.value)} />
            </label>
          </>
        )}

        <label className="field">
          <span>Data di scadenza</span>
          <input type="date" required value={expiryDate} onChange={(e) => setExpiryDate(e.target.value)} />
        </label>

        {user.isAdmin && (
          <label className="checkbox-field">
            <input type="checkbox" checked={special} onChange={(e) => setSpecial(e.target.checked)} />
            Documento riservato (visibile solo agli amministratori)
          </label>
        )}

        {error && <p className="form-error">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>Annulla</button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? "Caricamento..." : "Salva"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
