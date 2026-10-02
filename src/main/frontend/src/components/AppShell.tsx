import { useEffect, useState, type ReactNode } from "react";
import { Outlet, useLocation, useOutletContext } from "react-router";
import { DetailModal } from "../modals/DetailModal";
import { RenewModal } from "../modals/RenewModal";
import { UploadModal } from "../modals/UploadModal";
import type { DocumentRowHandlers } from "./DocumentsTable";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";

type ModalState =
  | { kind: "upload" }
  | { kind: "detail"; id: string }
  | { kind: "renew"; id: string }
  | null;

interface ShellContext {
  refreshKey: number;
  handlers: DocumentRowHandlers;
  openUpload: () => void;
}

export function AppShell() {
  const [refreshKey, setRefreshKey] = useState(0);
  const [modal, setModal] = useState<ModalState>(null);
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  const refresh = () => setRefreshKey((k) => k + 1);
  const closeModal = () => setModal(null);

  const context: ShellContext = {
    refreshKey,
    handlers: {
      onDetail: (id) => setModal({ kind: "detail", id }),
      onRenew: (id) => setModal({ kind: "renew", id }),
      onChanged: refresh
    },
    openUpload: () => setModal({ kind: "upload" })
  };

  return (
    <div className="app-shell">
      <Sidebar />

      <div className="main-column">
        <Outlet context={context} />
      </div>

      {modal?.kind === "upload" && <UploadModal onClose={closeModal} onSaved={refresh} />}
      {modal?.kind === "detail" && <DetailModal id={modal.id} onClose={closeModal} />}
      {modal?.kind === "renew" && <RenewModal id={modal.id} onClose={closeModal} onSaved={refresh} />}
    </div>
  );
}

export function useShell(): ShellContext {
  return useOutletContext<ShellContext>();
}

/** Topbar + area contenuto di una sezione. `upload`: mostra "+ Nuovo documento". */
export function Page({ title, upload, children }: { title: string; upload: boolean; children: ReactNode }) {
  const { openUpload } = useShell();
  return (
    <>
      <Topbar title={title} onUpload={upload ? openUpload : null} />
      <main className="content">{children}</main>
    </>
  );
}
