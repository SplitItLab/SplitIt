import { render, waitFor } from "@testing-library/react";
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

  it("no muestra contenido privado mientras valida la sesión", () => {
    vi.mocked(useSession).mockReturnValue({ status: "loading" });
    render(<Home />);
    expect(replace).not.toHaveBeenCalled();
  });

  it("redirige a /login cuando no hay sesión", async () => {
    vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
    render(<Home />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
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
