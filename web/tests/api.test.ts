import { afterEach, describe, expect, it, vi } from "vitest";
import { apiUrl } from "@/lib/api";

describe("apiUrl", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("usa path relativo cuando NEXT_PUBLIC_API_URL no está definido", () => {
    vi.stubEnv("NEXT_PUBLIC_API_URL", "");
    expect(apiUrl("/api/auth/register")).toBe("/api/auth/register");
  });

  it("antepone el host local si está configurado", () => {
    vi.stubEnv("NEXT_PUBLIC_API_URL", "http://localhost:8080/");
    expect(apiUrl("/api/auth/login")).toBe("http://localhost:8080/api/auth/login");
  });
});
