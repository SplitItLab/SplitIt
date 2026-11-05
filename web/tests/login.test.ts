import { describe, it, expect, vi, beforeEach } from "vitest";
import { loginSchema, login, loginWithGoogle, googleLoginErrorPath } from "../lib/auth";
import { ApiError } from "../lib/api";

vi.mock("../lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../lib/api")>();
  return {
    ...actual,
    request: vi.fn(),
  };
});

import { request } from "../lib/api";

describe("loginSchema", () => {
  it("acepta email y contraseña completos", () => {
    const result = loginSchema.safeParse({
      email: "ada@example.com",
      password: "una-clave-segura",
    });
    expect(result.success).toBe(true);
  });

  it("rechaza el email vacío", () => {
    const result = loginSchema.safeParse({ email: "", password: "una-clave-segura" });
    expect(result.success).toBe(false);
  });

  it("rechaza la contraseña vacía", () => {
    const result = loginSchema.safeParse({ email: "ada@example.com", password: "" });
    expect(result.success).toBe(false);
  });
});

describe("login", () => {
  const input = { email: "ada@example.com", password: "una-clave-segura" };

  beforeEach(() => {
    vi.mocked(request).mockReset();
  });

  it("no tira error con credenciales correctas", async () => {
    vi.mocked(request).mockResolvedValueOnce(undefined);
    await expect(login(input)).resolves.toBeUndefined();
  });

  it("traduce un 401 a invalid-credentials con mensaje genérico", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(401, "unauthorized"));
    await expect(login(input)).rejects.toMatchObject({ type: "invalid-credentials" });
  });

  it("traduce un 409 a una cuenta que ahora ingresa con Google", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(409, "conflict"));
    await expect(login(input)).rejects.toMatchObject({
      type: "google-account",
      message: "Esta cuenta ahora ingresa con Google",
    });
  });

  it("traduce un fallo de conexión a network", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(0, "no connection"));
    await expect(login(input)).rejects.toMatchObject({ type: "network" });
  });

  it("traduce un 500 a server-error", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(500, "boom"));
    await expect(login(input)).rejects.toMatchObject({ type: "server-error" });
  });
});

describe("loginWithGoogle", () => {
  beforeEach(() => {
    vi.mocked(request).mockReset();
  });

  it("manda la credencial de Google a la API", async () => {
    vi.mocked(request).mockResolvedValueOnce(undefined);

    await loginWithGoogle("id-token");

    expect(request).toHaveBeenCalledWith("/api/auth/google", {
      method: "POST",
      body: JSON.stringify({ credential: "id-token" }),
    });
  });

  it("propaga el rechazo de la API", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(401, "unauthorized"));
    await expect(loginWithGoogle("id-token")).rejects.toBeInstanceOf(ApiError);
  });
});

describe("googleLoginErrorPath", () => {
  it("vuelve al login con el error de Google", () => {
    expect(googleLoginErrorPath(null)).toBe("/login?error=google");
  });

  it("conserva la ruta pedida", () => {
    expect(googleLoginErrorPath("/eventos/invitacion/abc")).toBe(
      `/login?redirect=${encodeURIComponent("/eventos/invitacion/abc")}&error=google`
    );
  });
});
