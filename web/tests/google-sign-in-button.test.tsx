import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, waitFor, cleanup } from "@testing-library/react";

vi.mock("@/lib/auth", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/auth")>();
  return { ...actual, loginWithGoogle: vi.fn() };
});

import { loginWithGoogle } from "@/lib/auth";
import { GoogleSignInButton } from "@/components/google-sign-in-button";

type Callback = (response: { credential?: string }) => void;

function stubGoogleIdentity() {
  const identity = { initialize: vi.fn(), renderButton: vi.fn() };
  window.google = { accounts: { id: identity } };
  return identity;
}

async function renderAndGetCallback(onSuccess = vi.fn(), onError = vi.fn()) {
  const identity = stubGoogleIdentity();
  render(<GoogleSignInButton onSuccess={onSuccess} onError={onError} />);
  await waitFor(() => expect(identity.renderButton).toHaveBeenCalled());
  const callback = identity.initialize.mock.calls[0][0].callback as Callback;
  return { identity, callback, onSuccess, onError };
}

describe("GoogleSignInButton", () => {
  beforeEach(() => {
    vi.stubEnv("NEXT_PUBLIC_GOOGLE_CLIENT_ID", "client-id.apps.googleusercontent.com");
    vi.mocked(loginWithGoogle).mockReset();
    vi.mocked(loginWithGoogle).mockResolvedValue(undefined);
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllEnvs();
    delete window.google;
  });

  it("no ofrece Google cuando el ambiente no tiene client ID", () => {
    vi.stubEnv("NEXT_PUBLIC_GOOGLE_CLIENT_ID", "");
    const identity = stubGoogleIdentity();

    const { container } = render(<GoogleSignInButton onSuccess={vi.fn()} onError={vi.fn()} />);

    expect(container).toBeEmptyDOMElement();
    expect(identity.initialize).not.toHaveBeenCalled();
  });

  it("muestra el botón oficial de continuar con Google", async () => {
    const { identity } = await renderAndGetCallback();

    expect(identity.initialize).toHaveBeenCalledWith(
      expect.objectContaining({ client_id: "client-id.apps.googleusercontent.com" })
    );
    expect(identity.renderButton).toHaveBeenCalledWith(
      expect.any(HTMLElement),
      expect.objectContaining({ text: "continue_with", locale: "es" })
    );
  });

  it("con la credencial de Google inicia sesión en la API", async () => {
    const { callback, onSuccess, onError } = await renderAndGetCallback();

    callback({ credential: "id-token" });

    await waitFor(() => expect(onSuccess).toHaveBeenCalled());
    expect(loginWithGoogle).toHaveBeenCalledWith("id-token");
    expect(onError).not.toHaveBeenCalled();
  });

  it("avisa el error si la API rechaza el ingreso", async () => {
    vi.mocked(loginWithGoogle).mockRejectedValue(new Error("401"));
    const { callback, onSuccess, onError } = await renderAndGetCallback();

    callback({ credential: "id-token" });

    await waitFor(() => expect(onError).toHaveBeenCalled());
    expect(onSuccess).not.toHaveBeenCalled();
  });

  it("avisa el error si Google no devuelve credencial", async () => {
    const { callback, onError } = await renderAndGetCallback();

    callback({});

    await waitFor(() => expect(onError).toHaveBeenCalled());
    expect(loginWithGoogle).not.toHaveBeenCalled();
  });
});
