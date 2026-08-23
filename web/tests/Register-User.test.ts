import { describe, it, expect, vi, beforeEach } from "vitest";
import { registerUser } from "@/lib/auth";
import { ApiError } from "@/lib/api";

vi.mock("../lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../lib/api")>();
  return {
    ...actual,
    request: vi.fn(),
  };
});

import { request } from "@/lib/api";

describe("registerUser", () => {
  const input = { name: "Ada Lovelace", email: "ada@example.com", password: "una-clave-segura" };

  beforeEach(() => {
    vi.mocked(request).mockReset();
  });

  it("no tira error cuando el registro es exitoso", async () => {
    vi.mocked(request).mockResolvedValueOnce(undefined);
    await expect(registerUser(input)).resolves.toBeUndefined();
  });

  it("traduce un 409 a email-taken", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(409, "conflict"));
    await expect(registerUser(input)).rejects.toMatchObject({ type: "email-taken" });
  });

  it("traduce un 400 a invalid-data", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(400, "bad request"));
    await expect(registerUser(input)).rejects.toMatchObject({ type: "invalid-data" });
  });

  it("traduce un fallo de conexión (status 0) a network", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(0, "no connection"));
    await expect(registerUser(input)).rejects.toMatchObject({ type: "network" });
  });

  it("traduce un 500 a server-error", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(500, "boom"));
    await expect(registerUser(input)).rejects.toMatchObject({ type: "server-error" });
  });
});
