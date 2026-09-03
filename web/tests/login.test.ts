import { describe, it, expect, vi, beforeEach } from "vitest";
import { loginSchema, login } from "../lib/auth";
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

  it("traduce un fallo de conexión a network", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(0, "no connection"));
    await expect(login(input)).rejects.toMatchObject({ type: "network" });
  });

  it("traduce un 500 a server-error", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(500, "boom"));
    await expect(login(input)).rejects.toMatchObject({ type: "server-error" });
  });
});
