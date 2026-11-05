import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, cleanup, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

const replace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  usePathname: () => "/eventos/10",
}));

vi.mock("@/lib/use-session", () => ({ useSession: vi.fn() }));
vi.mock("@/lib/toast", () => ({ showAppToast: vi.fn() }));
vi.mock("@/lib/auth", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/auth")>();
  return { ...actual, logout: vi.fn() };
});

import { LogoutError, logout } from "@/lib/auth";
import { showAppToast } from "@/lib/toast";
import { useSession } from "@/lib/use-session";
import { AppHeader } from "../components/app-header";

const guest = { type: "guest" as const, eventId: 10, memberId: 2, name: "Francisco" };

describe("AppHeader", () => {
  beforeEach(() => {
    replace.mockClear();
    vi.mocked(logout).mockReset();
    vi.mocked(showAppToast).mockClear();
  });

  afterEach(() => {
    cleanup();
  });

  it("al usuario le muestra el menú con Eventos y Perfil", async () => {
    vi.mocked(useSession).mockReturnValue({
      status: "authenticated",
      user: { id: 1, name: "Ada", email: "ada@example.com" },
    });
    render(<AppHeader />);

    await userEvent.click(screen.getByRole("button", { name: "Menú de usuario" }));

    expect(await screen.findByRole("menuitem", { name: "Perfil" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Salir" })).not.toBeInTheDocument();
  });

  it("al invitado le muestra su nombre y Salir, sin Eventos ni Perfil", () => {
    vi.mocked(useSession).mockReturnValue({ status: "guest", guest });
    render(<AppHeader />);

    expect(screen.getByText("Francisco")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Salir" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Menú de usuario" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Split/ })).toHaveAttribute("href", "/eventos/10");
  });

  it("Salir cierra la sesión de invitado y vuelve al inicio", async () => {
    vi.mocked(useSession).mockReturnValue({ status: "guest", guest });
    vi.mocked(logout).mockResolvedValue(undefined);
    render(<AppHeader />);

    await userEvent.click(screen.getByRole("button", { name: "Salir" }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/"));
    expect(logout).toHaveBeenCalledTimes(1);
  });

  it("si Salir falla, muestra el error y se queda en el evento", async () => {
    vi.mocked(useSession).mockReturnValue({ status: "guest", guest });
    vi.mocked(logout).mockRejectedValue(
      new LogoutError("network", "No pudimos conectar con el servidor.")
    );
    render(<AppHeader />);

    await userEvent.click(screen.getByRole("button", { name: "Salir" }));

    await waitFor(() =>
      expect(showAppToast).toHaveBeenCalledWith("error", "No pudimos conectar con el servidor.")
    );
    expect(replace).not.toHaveBeenCalled();
  });
});
