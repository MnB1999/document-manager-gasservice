import { useShell } from "../components/AppShell";
import { DocumentsTable } from "../components/DocumentsTable";
import { Loaded } from "../components/StatusViews";
import { apiRequest } from "../lib/api";
import { useLoad } from "../lib/useLoad";
import type { DocumentItem } from "../types";

export function ExpiringView() {
  const { refreshKey, handlers } = useShell();
  const { state, retry } = useLoad(() => apiRequest<DocumentItem[]>("/documents/expiring"), [refreshKey]);

  return (
    <Loaded state={state} onRetry={retry}>
      {(docs) => (
        <div className="table-card">
          <DocumentsTable docs={docs} handlers={handlers} />
        </div>
      )}
    </Loaded>
  );
}
