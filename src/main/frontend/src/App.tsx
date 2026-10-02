import { Navigate, Route, Routes, useParams } from "react-router";
import { AppShell, Page } from "./components/AppShell";
import { LoginScreen } from "./components/LoginScreen";
import { Loading } from "./components/StatusViews";
import { useAuth } from "./context/AuthContext";
import { AuditView } from "./views/AuditView";
import { ExpiringView } from "./views/ExpiringView";
import { MyDocumentsView } from "./views/MyDocumentsView";
import { SearchView } from "./views/SearchView";

export function App() {
  const auth = useAuth();
  if (auth.status === "loading") return <Loading />;
  if (auth.status === "loggedOut") return <LoginScreen />;

  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Page title="Tutti i documenti" upload><SearchView category={null} /></Page>} />
        <Route path="miei" element={<Page title="I miei documenti" upload><MyDocumentsView /></Page>} />
        <Route path="scadenze" element={<Page title="In scadenza" upload><ExpiringView /></Page>} />
        <Route path="categoria/:nome" element={<CategoryPage />} />
        {}
        {auth.user.isAdmin && (
          <Route path="audit" element={<Page title="Registro audit" upload={false}><AuditView /></Page>} />
        )}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}

function CategoryPage() {
  const { nome } = useParams();
  if (!nome) throw new Error("Rotta categoria senza nome");
  return <Page title={nome} upload><SearchView category={nome} /></Page>;
}
