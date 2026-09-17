import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";

const replace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

vi.mock("@/lib/use-session", () => ({
  useSession: vi.fn(),
}));

import Home from "../app/page";
import { useSession } from "@/lib/use-session";

describe("Home", () => {
  beforeEach(() => {
    replace.mockReset();
    vi.mocked(useSession).mockReset();
  });

  it("no muestra contenido mientras valida la sesión", () => {
    vi.mocked(useSession).mockReturnValue({ status: "loading" });
    const { container } = render(<Home />);
    expect(container.querySelector("h1")).toBeNull();
    expect(replace).not.toHaveBeenCalled();
  });

  it("muestra la landing cuando no hay sesión", () => {
    vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
    render(<Home />);
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(
      "Compartí el link y listo."
    );
    expect(screen.getByRole("link", { name: /crear un evento/i })).toHaveAttribute(
      "href",
      "/register"
    );
    expect(replace).not.toHaveBeenCalled();
  });

  it("redirige a /eventos cuando hay sesión", async () => {
    vi.mocked(useSession).mockReturnValue({
      status: "authenticated",
      user: { id: 1, name: "Ada Lovelace", email: "ada@example.com" },
    });
    render(<Home />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/eventos"));
  });
});
