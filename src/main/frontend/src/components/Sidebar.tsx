import { NavLink } from "react-router";
import { CONFIG } from "../config";
import { useCurrentUser } from "../context/AuthContext";

export function Sidebar() {
  const user = useCurrentUser();

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <span className="brand-mark small">GS</span>
      </div>

      <nav className="sidebar-nav">
        {/* "end": attiva solo su "/" esatto, non su ogni percorso che inizia con "/" */}
        <NavLink to="/" end className="nav-item">Tutti i documenti</NavLink>
        <NavLink to="/miei" className="nav-item">I miei documenti</NavLink>
        <NavLink to="/scadenze" className="nav-item">In scadenza</NavLink>

        <div className="nav-section-label">Categorie</div>
        {CONFIG.CATEGORIES.map((c) => (
          <NavLink key={c} to={`/categoria/${encodeURIComponent(c)}`} className="nav-item">{c}</NavLink>
        ))}

        {user.isAdmin && <NavLink to="/audit" className="nav-item nav-item-admin">Registro audit</NavLink>}
      </nav>
    </aside>
  );
}
