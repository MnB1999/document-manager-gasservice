import { useEffect, useState, type DependencyList } from "react";

export type LoadState<T> =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; data: T };


export function useLoad<T>(load: () => Promise<T>, deps: DependencyList) {
  const [state, setState] = useState<LoadState<T>>({ status: "loading" });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setState({ status: "loading" });
    load().then(
      (data) => { if (!cancelled) setState({ status: "ready", data }); },
      (err: Error) => { if (!cancelled) setState({ status: "error", message: err.message }); }
    );
    return () => { cancelled = true; };
  }, [...deps, attempt]);

  return { state, retry: () => setAttempt((a) => a + 1) };
}
