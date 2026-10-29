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

vi.mock("@/lib/toast", () => ({ showAppToast: vi.fn() }));

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
  return { ...actual, registerUser: vi.fn() };
});

import { registerUser } from "@/lib/auth";
import RegisterPage from "../app/register/page";

const INVITATION_PATH = "/eventos/invitacion/abc";
const LOGIN_WITH_REDIRECT = `/login?redirect=${encodeURIComponent(INVITATION_PATH)}`;

async function submitRegister() {
  const user = userEvent.setup();
  render(<RegisterPage />);
  await user.type(screen.getByLabelText("Nombre completo"), "Sofia");
  await user.type(screen.getByLabelText("Email"), "sofia@email.com");
  await user.type(screen.getByLabelText("Contraseña"), "una-clave-segura");
  await user.click(screen.getByRole("button", { name: "Crear cuenta" }));
}

describe("RegisterPage", () => {
  beforeEach(() => {
    push.mockClear();
    vi.mocked(registerUser).mockReset();
    vi.mocked(registerUser).mockResolvedValue(undefined);
  });

  afterEach(() => {
    cleanup();
    window.history.replaceState(null, "", "/");
  });

  it("después de registrarse vuelve al login con la ruta pedida", async () => {
    window.history.replaceState(
      null,
      "",
      `/register?redirect=${encodeURIComponent(INVITATION_PATH)}`
    );

    await submitRegister();

    await waitFor(() => expect(push).toHaveBeenCalledWith(LOGIN_WITH_REDIRECT));
  });

  it("el link para iniciar sesión conserva la ruta pedida", () => {
    window.history.replaceState(
      null,
      "",
      `/register?redirect=${encodeURIComponent(INVITATION_PATH)}`
    );
    render(<RegisterPage />);

    expect(screen.getByRole("link", { name: "Iniciá sesión" })).toHaveAttribute(
      "href",
      LOGIN_WITH_REDIRECT
    );
  });

  it("sin ruta pedida vuelve al login como siempre", async () => {
    window.history.replaceState(null, "", "/register");

    await submitRegister();

    await waitFor(() => expect(push).toHaveBeenCalledWith("/login"));
  });

  it("ignora una ruta pedida hacia otro sitio", async () => {
    window.history.replaceState(
      null,
      "",
      `/register?redirect=${encodeURIComponent("https://otro-sitio.com")}`
    );

    await submitRegister();

    await waitFor(() => expect(push).toHaveBeenCalledWith("/login"));
  });

  it("después de ingresar con Google entra directo a la ruta pedida", async () => {
    window.history.replaceState(
      null,
      "",
      `/register?redirect=${encodeURIComponent(INVITATION_PATH)}`
    );
    const user = userEvent.setup();
    render(<RegisterPage />);

    await user.click(screen.getByRole("button", { name: "Google ok" }));

    expect(push).toHaveBeenCalledWith(INVITATION_PATH);
  });

  // SPLT-019: si se cancela o falla Google, vuelve al inicio de sesión con el mensaje.
  it("si falla el ingreso con Google vuelve al login con el error", async () => {
    window.history.replaceState(
      null,
      "",
      `/register?redirect=${encodeURIComponent(INVITATION_PATH)}`
    );
    const user = userEvent.setup();
    render(<RegisterPage />);

    await user.click(screen.getByRole("button", { name: "Google falla" }));

    expect(push).toHaveBeenCalledWith(`${LOGIN_WITH_REDIRECT}&error=google`);
  });
});
