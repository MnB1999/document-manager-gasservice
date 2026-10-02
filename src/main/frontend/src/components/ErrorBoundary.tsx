import { Component, type ReactNode } from "react";

export class ErrorBoundary extends Component<{ children: ReactNode }, { error: Error | null }> {
  state = { error: null as Error | null };

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  render() {
    if (this.state.error) {
      return (
        <div className="error-state">
          <span>Si è verificato un errore imprevisto: {this.state.error.message}</span>
          <button className="btn btn-secondary" onClick={() => window.location.reload()}>Ricarica la pagina</button>
        </div>
      );
    }
    return this.props.children;
  }
}
