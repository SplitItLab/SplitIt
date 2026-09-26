import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";

vi.mock("@/lib/use-session", () => ({
  useSession: vi.fn(),
}));

import NotFound from "../app/not-found";
import { useSession } from "@/lib/use-session";

describe("NotFound", () => {
  beforeEach(() => {
    vi.mocked(useSession).mockReset();
  });

  it("manda al login cuando no hay sesión", () => {
    vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
    render(<NotFound />);

    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(
      "Esta página se fue de viaje"
    );
    expect(screen.getByRole("link", { name: "Volver al inicio" })).toHaveAttribute(
      "href",
      "/login"
    );
  });

  it("manda a /eventos cuando hay sesión", () => {
    vi.mocked(useSession).mockReturnValue({
      status: "authenticated",
      user: { id: 1, name: "Ada Lovelace", email: "ada@example.com" },
    });
    render(<NotFound />);

    expect(screen.getByRole("link", { name: "Volver al inicio" })).toHaveAttribute(
      "href",
      "/eventos"
    );
  });

  it("manda al login mientras la sesión está cargando", () => {
    vi.mocked(useSession).mockReturnValue({ status: "loading" });
    render(<NotFound />);

    expect(screen.getByRole("link", { name: "Volver al inicio" })).toHaveAttribute(
      "href",
      "/login"
    );
  });
});
