import { describe, it, expect, vi, beforeEach } from "vitest";
import { getSession, logout, LogoutError } from "../lib/auth";
import { ApiError } from "../lib/api";

vi.mock("../lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../lib/api")>();
  return {
    ...actual,
    request: vi.fn(),
  };
});

import { request } from "../lib/api";

describe("getSession", () => {
  beforeEach(() => {
    vi.mocked(request).mockReset();
  });

  it("devuelve el usuario cuando hay una sesión activa (restaurada tras recarga)", async () => {
    const user = { id: 1, name: "Ada Lovelace", email: "ada@example.com" };
    vi.mocked(request).mockResolvedValueOnce(user);

    await expect(getSession()).resolves.toEqual(user);
  });

  it("devuelve null cuando no hay sesión (401)", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(401, "unauthorized"));
    await expect(getSession()).resolves.toBeNull();
  });
});

describe("logout", () => {
  beforeEach(() => {
    vi.mocked(request).mockReset();
  });

  it("resuelve sin lanzar cuando el logout es exitoso", async () => {
    vi.mocked(request).mockResolvedValueOnce(undefined);
    await expect(logout()).resolves.toBeUndefined();
  });

  it("lanza LogoutError de tipo network ante un error de red", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(0, "No pudimos conectar con la API."));

    const error = await logout().catch((err) => err);
    expect(error).toBeInstanceOf(LogoutError);
    expect((error as LogoutError).type).toBe("network");
  });

  it("lanza LogoutError de tipo server-error ante un error del servidor", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(500, "Internal Server Error"));

    const error = await logout().catch((err) => err);
    expect(error).toBeInstanceOf(LogoutError);
    expect((error as LogoutError).type).toBe("server-error");
  });
});
