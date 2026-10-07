import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { LoadingIndicator } from "@/components/loading-indicator";

describe("LoadingIndicator", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("anuncia la carga en español desde el inicio", () => {
    render(<LoadingIndicator message="Verificando tu sesión…" />);

    expect(screen.getByRole("status", { name: "Cargando" })).toBeInTheDocument();
  });

  it("no muestra el mensaje antes de un segundo", () => {
    render(<LoadingIndicator message="Verificando tu sesión…" />);

    act(() => {
      vi.advanceTimersByTime(999);
    });

    expect(screen.queryByText("Verificando tu sesión…")).not.toBeInTheDocument();
  });

  it("muestra el mensaje de contexto cuando la carga supera un segundo", () => {
    render(<LoadingIndicator message="Verificando tu sesión…" />);

    act(() => {
      vi.advanceTimersByTime(1000);
    });

    expect(screen.getByText("Verificando tu sesión…")).toBeInTheDocument();
  });

  it("cancela el temporizador al desmontarse", () => {
    const { unmount } = render(<LoadingIndicator message="Verificando tu sesión…" />);

    unmount();

    expect(vi.getTimerCount()).toBe(0);
  });
});
