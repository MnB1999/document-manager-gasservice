import { CONFIG } from "../config";
import { useShell } from "../components/AppShell";
import { DocumentsTable } from "../components/DocumentsTable";
import { Pagination } from "../components/Pagination";
import { Loaded } from "../components/StatusViews";
import { useCurrentUser } from "../context/AuthContext";
import { apiRequest } from "../lib/api";
import { usePageParam } from "../lib/urlParams";
import { useLoad } from "../lib/useLoad";
import type { DocumentItem, Page } from "../types";

export function MyDocumentsView() {
  const user = useCurrentUser();
  const { refreshKey, handlers } = useShell();
  const [page, setPage] = usePageParam();

  const { state, retry } = useLoad(
    () => apiRequest<Page<DocumentItem>>(`/documents/user/${user.supabaseId}`, {
      query: { page, size: CONFIG.PAGE_SIZE }
    }),
    [page, user.supabaseId, refreshKey]
  );

  return (
    <Loaded state={state} onRetry={retry}>
      {(data) => (
        <div className="table-card">
          <DocumentsTable docs={data.content} handlers={handlers} />
          <Pagination page={data.page} onChange={setPage} />
        </div>
      )}
    </Loaded>
  );
}
