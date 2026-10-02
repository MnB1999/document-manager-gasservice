import { CONFIG } from "../config";
import { Pagination } from "../components/Pagination";
import { Loaded } from "../components/StatusViews";
import { apiRequest } from "../lib/api";
import { auditActionLabel, formatDateTime } from "../lib/format";
import { usePageParam } from "../lib/urlParams";
import { useLoad } from "../lib/useLoad";
import type { AuditLog, Page } from "../types";

export function AuditView() {
  const [page, setPage] = usePageParam();
  const { state, retry } = useLoad(
    () => apiRequest<Page<AuditLog>>("/audit", { query: { page, size: CONFIG.PAGE_SIZE } }),
    [page]
  );

  return (
    <Loaded state={state} onRetry={retry}>
      {(data) => (
        <div className="table-card">
          {data.content.length === 0 ? (
            <div className="empty-state">Nessuna operazione registrata.</div>
          ) : (
            <table>
              <thead><tr><th>Documento</th><th>Azione</th><th>Utente</th><th>Data</th></tr></thead>
              <tbody>
                {data.content.map((log) => (
                  <tr key={log.id}>
                    <td>{log.documentTitle || log.documentId}</td>
                    <td>{auditActionLabel(log.action)}</td>
                    <td>{log.userEmail ?? "Sistema"}</td>
                    <td>{formatDateTime(log.performedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <Pagination page={data.page} onChange={setPage} />
        </div>
      )}
    </Loaded>
  );
}
