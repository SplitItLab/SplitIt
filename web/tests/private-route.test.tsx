import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";

const replace = vi.fn();
let pathname = "/eventos/10";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  usePathname: () => pathname,
}));

vi.mock("@/lib/use-session", () => ({ useSession: vi.fn() }));

import { useSession } from "@/lib/use-session";
import { PrivateRoute } from "../components/private-route";

const guest = { type: "guest" as const, eventId: 10, memberId: 2, name: "Francisco" };

function renderRoute() {
  return render(
    <PrivateRoute>
      <p>contenido privado</p>
    </PrivateRoute>
  );
}

describe("PrivateRoute", () => {
  beforeEach(() => {
    replace.mockClear();
    pathname = "/eventos/10";
    localStorage.clear();
  });

  afterEach(() => {
    cleanup();
  });

  it("manda al login a quien no tiene sesión", () => {
    vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
    renderRoute();

    expect(replace).toHaveBeenCalledWith("/login");
    expect(screen.queryByText("contenido privado")).not.toBeInTheDocument();
  });

  it("vuelve a la invitación si el invitado perdió el acceso a su evento", () => {
    localStorage.setItem("splitit:guest-event:10", "tok-123");
    vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
    renderRoute();

    expect(replace).toHaveBeenCalledWith("/eventos/invitacion/tok-123");
    expect(replace).not.toHaveBeenCalledWith("/login");
  });

  it("manda al login si la invitación recordada es de otro evento", () => {
    localStorage.setItem("splitit:guest-event:11", "tok-123");
    vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
    renderRoute();

    expect(replace).toHaveBeenCalledWith("/login");
  });

  it("deja pasar al usuario a cualquier ruta", () => {
    vi.mocked(useSession).mockReturnValue({
      status: "authenticated",
      user: { id: 1, name: "Ada", email: "ada@example.com" },
    });
    pathname = "/perfil";
    renderRoute();

    expect(screen.getByText("contenido privado")).toBeInTheDocument();
    expect(replace).not.toHaveBeenCalled();
  });

  it("deja pasar al invitado en su evento", () => {
    vi.mocked(useSession).mockReturnValue({ status: "guest", guest });
    renderRoute();

    expect(screen.getByText("contenido privado")).toBeInTheDocument();
    expect(replace).not.toHaveBeenCalled();
  });

  it.each(["/eventos", "/perfil", "/dashboard", "/eventos/11", "/eventos/100"])(
    "manda al invitado a su evento si abre %s",
    (path) => {
      vi.mocked(useSession).mockReturnValue({ status: "guest", guest });
      pathname = path;
      renderRoute();

      expect(replace).toHaveBeenCalledWith("/eventos/10");
      expect(screen.queryByText("contenido privado")).not.toBeInTheDocument();
    }
  );
});
