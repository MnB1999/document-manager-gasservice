import { useNavigate } from "react-router";
import { useCurrentUser } from "../context/AuthContext";
import { logout } from "../lib/auth";

export function Topbar({ title, onUpload }: { title: string; onUpload: (() => void) | null }) {
  const user = useCurrentUser();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate("/");
  }

  return (
    <header className="topbar">
      <h1 className="topbar-title">{title}</h1>
      <div className="topbar-right">
        {onUpload && <button className="btn btn-primary" onClick={onUpload}>+ Nuovo documento</button>}
        <div className="account-box">
          <span className="account-email">{user.email ?? ""}</span>
          <span className={`role-badge ${user.isAdmin ? "admin" : ""}`}>{user.role}</span>
          <button className="btn btn-secondary" onClick={handleLogout}>Esci</button>
        </div>
      </div>
    </header>
  );
}
