import { describe, it, expect, vi, beforeEach } from "vitest";
import { getSession } from "../lib/auth";
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
