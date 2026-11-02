import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor, cleanup } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

const push = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
  useSearchParams: () => new URLSearchParams(window.location.search),
}));

vi.mock("next/image", () => ({
  // Test double for next/image.
  // eslint-disable-next-line @next/next/no-img-element
  default: ({ src }: { src: string }) => <img src={src} alt="" />,
}));

vi.mock("@/components/google-sign-in-button", () => ({
  GoogleSignInButton: ({ onSuccess, onError }: { onSuccess: () => void; onError: () => void }) => (
    <>
      <button onClick={onSuccess}>Google ok</button>
      <button onClick={onError}>Google falla</button>
    </>
  ),
}));

vi.mock("@/lib/auth", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/auth")>();
  return { ...actual, login: vi.fn() };
});

import { login, LoginError } from "@/lib/auth";
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

  it("el link para registrarse conserva la ruta pedida", () => {
    window.history.replaceState(
      null,
      "",
      `/login?redirect=${encodeURIComponent("/eventos/invitacion/abc")}`
    );
    render(<LoginPage />);

    expect(screen.getByRole("link", { name: "Registrate gratis" })).toHaveAttribute(
      "href",
      `/register?redirect=${encodeURIComponent("/eventos/invitacion/abc")}`
    );
  });

  it("ignora una ruta pedida hacia otro sitio", async () => {
    window.history.replaceState(
      null,
      "",
      `/login?redirect=${encodeURIComponent("//otro-sitio.com")}`
    );

    await submitLogin();

    await waitFor(() => expect(push).toHaveBeenCalledWith("/eventos"));
    expect(screen.getByRole("link", { name: "Registrate gratis" })).toHaveAttribute(
      "href",
      "/register"
    );
  });

  it("sin ruta pedida va a mis eventos", async () => {
    window.history.replaceState(null, "", "/login");

    await submitLogin();

    await waitFor(() => expect(push).toHaveBeenCalledWith("/eventos"));
  });

  // SPLT-019: el usuario que intenta con contraseña una cuenta de Google ve el aviso.
  it("con contraseña sobre una cuenta de Google muestra que ahora ingresa con Google", async () => {
    vi.mocked(login).mockRejectedValue(
      new LoginError("google-account", "Esta cuenta ahora ingresa con Google")
    );

    await submitLogin();

    expect(await screen.findByText("Esta cuenta ahora ingresa con Google")).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });

  it("después de ingresar con Google vuelve a la ruta pedida", async () => {
    window.history.replaceState(
      null,
      "",
      `/login?redirect=${encodeURIComponent("/eventos/invitacion/abc")}`
    );
    const user = userEvent.setup();
    render(<LoginPage />);

    await user.click(screen.getByRole("button", { name: "Google ok" }));

    expect(push).toHaveBeenCalledWith("/eventos/invitacion/abc");
  });

  it("si falla el ingreso con Google muestra el error sin salir del login", async () => {
    const user = userEvent.setup();
    render(<LoginPage />);

    await user.click(screen.getByRole("button", { name: "Google falla" }));

    expect(screen.getByText("No pudimos completar el ingreso con Google")).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });

  it("al volver de un ingreso con Google fallido muestra el error", () => {
    window.history.replaceState(null, "", "/login?error=google");
    render(<LoginPage />);

    expect(screen.getByText("No pudimos completar el ingreso con Google")).toBeInTheDocument();
  });
});
