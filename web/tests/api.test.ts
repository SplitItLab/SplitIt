import { afterEach, describe, expect, it, vi } from "vitest";
import { apiUrl, request } from "@/lib/api";

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

describe("request", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("propaga el aborto sin convertirlo en un error de red", async () => {
    const controller = new AbortController();
    vi.stubGlobal(
      "fetch",
      vi.fn((_url: string, init?: RequestInit) => {
        return new Promise((_resolve, reject) => {
          init?.signal?.addEventListener("abort", () => {
            reject(new DOMException("The operation was aborted.", "AbortError"));
          });
        });
      })
    );

    const pending = request("/api/events/7/expenses/quote", { signal: controller.signal });
    controller.abort();

    await expect(pending).rejects.toMatchObject({ name: "AbortError" });
  });
});
