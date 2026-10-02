import type { Page } from "../types";

export function Pagination({ page, onChange }: { page: Page<unknown>["page"]; onChange: (page: number) => void }) {
  if (page.totalPages <= 1) return null;
  const first = page.number <= 0;
  const last = page.number >= page.totalPages - 1;

  return (
    <div className="pagination">
      <span>Pagina {page.number + 1} di {page.totalPages} — {page.totalElements} documenti</span>
      <button className="btn btn-secondary" disabled={first} onClick={() => onChange(page.number - 1)}>Precedente</button>
      <button className="btn btn-secondary" disabled={last} onClick={() => onChange(page.number + 1)}>Successiva</button>
    </div>
  );
}
