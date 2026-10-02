import { useState, type SubmitEvent } from "react";
import { CONFIG } from "../config";
import { login } from "../lib/auth";

export function LoginScreen() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(email.trim(), password);
    } catch (err) {
      setError((err as Error).message);
      setSubmitting(false);
    }
  }

  return (
    <div className="login-screen">
      <div className="login-screen-left">
        <div className="brand">
          <span className="brand-mark">GS</span>
          <span className="brand-name">Gas Service</span>
        </div>
        <p className="brand-tag">Archivio documentale aziendale — certificazioni, contratti e scadenze in un unico posto.</p>

        <div className="category-preview-label">Categorie disponibili</div>
        <ul className="category-preview">
          {CONFIG.CATEGORIES.map((c) => <li key={c}>{c}</li>)}
        </ul>
      </div>

      <div className="login-screen-right">
        <form className="login-card" autoComplete="on" onSubmit={handleSubmit}>
          <h1>Accedi</h1>
          <p className="login-sub">Inserisci le credenziali del tuo account aziendale.</p>

          <label className="field">
            <span>Email</span>
            <input type="email" required autoComplete="username" placeholder="nome.cognome@azienda.it"
              value={email} onChange={(e) => setEmail(e.target.value)} />
          </label>

          <label className="field">
            <span>Password</span>
            <input type="password" required autoComplete="current-password" placeholder="••••••••"
              value={password} onChange={(e) => setPassword(e.target.value)} />
          </label>

          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? "Accesso in corso..." : "Accedi"}
          </button>

          {error && <p className="form-error">{error}</p>}
        </form>
      </div>
    </div>
  );
}
