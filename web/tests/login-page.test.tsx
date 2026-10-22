import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor, cleanup } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

const push = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
}));

vi.mock("next/image", () => ({
  // Test double for next/image.
  // eslint-disable-next-line @next/next/no-img-element
  default: ({ src }: { src: string }) => <img src={src} alt="" />,
}));

vi.mock("@/lib/auth", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/auth")>();
  return { ...actual, login: vi.fn() };
});

import { login } from "@/lib/auth";
import LoginPage from "../app/login/page";

async function submitLogin() {
  const user = userEvent.setup();
  render(<LoginPage />);
  await user.type(screen.getByLabelText("Email"), "sofia@email.com");
  await user.type(screen.getByLabelText("Contraseña"), "una-clave-segura");
  await user.click(screen.getByRole("button", { name: "Ingresar" }));
}

describe("LoginPage", () => {
  beforeEach(() => {
    push.mockClear();
    vi.mocked(login).mockReset();
    vi.mocked(login).mockResolvedValue(undefined);
  });

  afterEach(() => {
    cleanup();
    window.history.replaceState(null, "", "/");
  });

  it("después de iniciar sesión vuelve a la ruta pedida", async () => {
    window.history.replaceState(
      null,
      "",
      `/login?redirect=${encodeURIComponent("/eventos/invitacion/abc")}`
    );

    await submitLogin();

    await waitFor(() => expect(push).toHaveBeenCalledWith("/eventos/invitacion/abc"));
  });

  it("sin ruta pedida va a mis eventos", async () => {
    window.history.replaceState(null, "", "/login");

    await submitLogin();

    await waitFor(() => expect(push).toHaveBeenCalledWith("/eventos"));
  });
});
