import { useSearchParams } from "react-router";

/** Pagina corrente, 0-based per il backend. Nell'URL è 1-based: ?pagina=2 */
export function usePageParam(): [number, (page: number) => void] {
  const [params, setParams] = useSearchParams();
  const n = Number(params.get("pagina"));
  const page = Number.isInteger(n) && n > 1 ? n - 1 : 0;

  function setPage(next: number) {
    setParams((prev) => {
      const updated = new URLSearchParams(prev);
      if (next === 0) updated.delete("pagina");
      else updated.set("pagina", String(next + 1));
      return updated;
    });
  }

  return [page, setPage];
}

export interface Filters {
  title: string;
  startDate: string;
  endDate: string;
}

/** Filtri di ricerca: ?titolo=...&da=YYYY-MM-DD&a=YYYY-MM-DD */
export function useFiltersParam(): [Filters, (filters: Filters) => void] {
  const [params, setParams] = useSearchParams();
  const filters: Filters = {
    title: params.get("titolo") ?? "",
    startDate: params.get("da") ?? "",
    endDate: params.get("a") ?? ""
  };

  function setFilters(next: Filters) {
    const updated = new URLSearchParams();
    if (next.title) updated.set("titolo", next.title);
    if (next.startDate) updated.set("da", next.startDate);
    if (next.endDate) updated.set("a", next.endDate);
    setParams(updated);
  }

  return [filters, setFilters];
}
