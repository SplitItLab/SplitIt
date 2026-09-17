import { renderHook, waitFor, render, screen } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { useSession } from "../lib/use-session";
import { getSession } from "../lib/auth";
import Home from "../app/page";

const replace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

vi.mock("../lib/auth", () => ({
  getSession: vi.fn(),
}));

describe("useSession", () => {
  beforeEach(() => {
    replace.mockReset();
    vi.mocked(getSession).mockReset();
  });

  it("inicia en loading y resuelve a authenticated cuando getSession devuelve un usuario", async () => {
    const user = { id: 1, name: "Ada Lovelace", email: "ada@example.com" };
    vi.mocked(getSession).mockResolvedValueOnce(user);

    const { result } = renderHook(() => useSession());
    expect(result.current).toEqual({ status: "loading" });

    await waitFor(() => {
      expect(result.current).toEqual({ status: "authenticated", user });
    });
  });

  it("resuelve a unauthenticated cuando getSession devuelve null", async () => {
    vi.mocked(getSession).mockResolvedValueOnce(null);

    const { result } = renderHook(() => useSession());
    expect(result.current).toEqual({ status: "loading" });

    await waitFor(() => {
      expect(result.current).toEqual({ status: "unauthenticated" });
    });
  });

  it("resuelve a unauthenticated cuando getSession rechaza (ej. error 500 o fallo de red)", async () => {
    vi.mocked(getSession).mockRejectedValueOnce(new Error("Network / Server error"));

    const { result } = renderHook(() => useSession());
    expect(result.current).toEqual({ status: "loading" });

    await waitFor(() => {
      expect(result.current).toEqual({ status: "unauthenticated" });
    });
  });

  it("no actualiza el estado si el componente se desmonta antes de que la promesa resuelva", async () => {
    let resolvePromise!: (value: { id: number; name: string; email: string }) => void;
    vi.mocked(getSession).mockReturnValueOnce(
      new Promise((resolve) => {
        resolvePromise = resolve;
      })
    );

    const { result, unmount } = renderHook(() => useSession());
    expect(result.current).toEqual({ status: "loading" });

    unmount();
    resolvePromise({ id: 1, name: "Ada Lovelace", email: "ada@example.com" });

    expect(result.current).toEqual({ status: "loading" });
  });

  it("no actualiza el estado si el componente se desmonta antes de que la promesa falle", async () => {
    let rejectPromise!: (reason: unknown) => void;
    vi.mocked(getSession).mockReturnValueOnce(
      new Promise((_, reject) => {
        rejectPromise = reject;
      })
    );

    const { result, unmount } = renderHook(() => useSession());
    expect(result.current).toEqual({ status: "loading" });

    unmount();
    rejectPromise(new Error("Network failed"));

    expect(result.current).toEqual({ status: "loading" });
  });
});

describe("Landing page fallback con backend no disponible", () => {
  beforeEach(() => {
    replace.mockReset();
    vi.mocked(getSession).mockReset();
  });

  it("renderiza la landing pública cuando getSession rechaza por fallo de backend o red", async () => {
    vi.mocked(getSession).mockRejectedValueOnce(new Error("Backend unavailable"));

    render(<Home />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(
        "Compartí el link y listo."
      );
    });

    expect(screen.getByRole("link", { name: /crear un evento/i })).toHaveAttribute(
      "href",
      "/register"
    );
    expect(replace).not.toHaveBeenCalled();
  });
});
