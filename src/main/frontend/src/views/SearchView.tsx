import { useState } from "react";
import { useLocation } from "react-router";
import { CONFIG } from "../config";
import { useShell } from "../components/AppShell";
import { DocumentsTable } from "../components/DocumentsTable";
import { Pagination } from "../components/Pagination";
import { Loaded } from "../components/StatusViews";
import { apiRequest } from "../lib/api";
import { useFiltersParam, usePageParam, type Filters } from "../lib/urlParams";
import { useLoad } from "../lib/useLoad";
import type { DocumentItem, Page } from "../types";

const EMPTY_FILTERS: Filters = { title: "", startDate: "", endDate: "" };

export function SearchView({ category }: { category: string | null }) {
  const { refreshKey, handlers } = useShell();
  const [page, setPage] = usePageParam();
  const [filters, setFilters] = useFiltersParam();
  const location = useLocation();

  const { state, retry } = useLoad(
    () => apiRequest<Page<DocumentItem>>("/documents/search", {
      query: {
        page,
        size: CONFIG.PAGE_SIZE,
        title: filters.title,
        category,
        startDate: filters.startDate,
        endDate: filters.endDate
      }
    }),
    [page, filters.title, filters.startDate, filters.endDate, category, refreshKey]
  );

  return (
    <>
      {/* La key riallinea i campi all'URL quando cambia (Indietro, cambio categoria, ecc.) */}
      <FilterToolbar key={location.pathname + location.search} applied={filters} onApply={setFilters} />

      <Loaded state={state} onRetry={retry}>
        {(data) => (
          <div className="table-card">
            <DocumentsTable docs={data.content} handlers={handlers} />
            <Pagination page={data.page} onChange={setPage} />
          </div>
        )}
      </Loaded>
    </>
  );
}

function FilterToolbar({ applied, onApply }: { applied: Filters; onApply: (filters: Filters) => void }) {
  const [draft, setDraft] = useState<Filters>(applied); // valori nei campi, non ancora applicati
  const hasFilters = applied.title !== "" || applied.startDate !== "" || applied.endDate !== "";

  return (
    <div className="toolbar">
      <label className="field">
        <span>Titolo</span>
        <input type="text" placeholder="Cerca per titolo" value={draft.title}
          onChange={(e) => setDraft({ ...draft, title: e.target.value })} />
      </label>
      <label className="field">
        <span>Da</span>
        <input type="date" value={draft.startDate} onChange={(e) => setDraft({ ...draft, startDate: e.target.value })} />
      </label>
      <label className="field">
        <span>A</span>
        <input type="date" value={draft.endDate} onChange={(e) => setDraft({ ...draft, endDate: e.target.value })} />
      </label>
      <button className="btn btn-secondary" onClick={() => onApply({ ...draft, title: draft.title.trim() })}>Filtra</button>
      {hasFilters && <button className="btn btn-secondary" onClick={() => onApply(EMPTY_FILTERS)}>Azzera</button>}
    </div>
  );
}
