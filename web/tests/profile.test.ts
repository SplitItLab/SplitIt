import { describe, it, expect, vi, beforeEach } from "vitest";
import { profileSchema, getProfile, updateProfile, getInitials } from "../lib/profile";
import { ApiError } from "../lib/api";

vi.mock("../lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../lib/api")>();
  return {
    ...actual,
    request: vi.fn(),
  };
});

import { request } from "../lib/api";

describe("profileSchema", () => {
  it("acepta nombre y email completos", () => {
    const result = profileSchema.safeParse({ name: "Ada Lovelace", email: "ada@example.com" });
    expect(result.success).toBe(true);
  });

  it("rechaza el nombre vacío", () => {
    const result = profileSchema.safeParse({ name: "", email: "ada@example.com" });
    expect(result.success).toBe(false);
  });

  it("rechaza el nombre compuesto solo por espacios", () => {
    const result = profileSchema.safeParse({ name: "   ", email: "ada@example.com" });
    expect(result.success).toBe(false);
  });

  it("rechaza un email con formato inválido", () => {
    const result = profileSchema.safeParse({ name: "Ada Lovelace", email: "no-es-un-email" });
    expect(result.success).toBe(false);
  });
});

describe("getProfile", () => {
  beforeEach(() => {
    vi.mocked(request).mockReset();
  });

  it("devuelve el perfil del usuario autenticado", async () => {
    const profile = { id: "1", name: "Ada Lovelace", email: "ada@example.com" };
    vi.mocked(request).mockResolvedValueOnce(profile);
    await expect(getProfile()).resolves.toEqual(profile);
  });

  it("traduce un 401 a unauthorized", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(401, "unauthorized"));
    await expect(getProfile()).rejects.toMatchObject({ type: "unauthorized" });
  });
});

describe("updateProfile", () => {
  const input = { name: "Ada Byron Lovelace", email: "ada.lovelace@example.com" };

  beforeEach(() => {
    vi.mocked(request).mockReset();
  });

  it("devuelve el perfil actualizado", async () => {
    const profile = { id: "1", ...input };
    vi.mocked(request).mockResolvedValueOnce(profile);
    await expect(updateProfile(input)).resolves.toEqual(profile);
  });

  it("traduce un 409 a email-taken apuntando al campo email", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(409, "email taken"));
    await expect(updateProfile(input)).rejects.toMatchObject({
      type: "email-taken",
      field: "email",
    });
  });

  it("traduce un 401 a unauthorized", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(401, "unauthorized"));
    await expect(updateProfile(input)).rejects.toMatchObject({ type: "unauthorized" });
  });

  it("traduce un fallo de conexión a network", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(0, "no connection"));
    await expect(updateProfile(input)).rejects.toMatchObject({ type: "network" });
  });

  it("traduce un 500 a server-error", async () => {
    vi.mocked(request).mockRejectedValueOnce(new ApiError(500, "boom"));
    await expect(updateProfile(input)).rejects.toMatchObject({ type: "server-error" });
  });
});

describe("getInitials", () => {
  it("combina la primera letra del nombre y del apellido", () => {
    expect(getInitials("Ada Lovelace")).toBe("AL");
  });

  it("usa una sola letra cuando hay un único nombre", () => {
    expect(getInitials("Ada")).toBe("A");
  });

  it("ignora espacios extra", () => {
    expect(getInitials("  Ada   Byron Lovelace  ")).toBe("AL");
  });
});
