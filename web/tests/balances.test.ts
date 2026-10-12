import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { listBalances, type MemberBalance } from "../lib/balances";

const ana: MemberBalance = {
  memberId: 1,
  memberName: "Ana",
  netBalance: 50,
};

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("listBalances", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("pide GET /api/events/:id/balances y devuelve el saldo de cada integrante", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse([ana]));

    await expect(listBalances(7)).resolves.toEqual([ana]);

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(String(url)).toContain("/api/events/7/balances");
    expect(init?.method ?? "GET").toBe("GET");
  });

  it("traduce un 503 al mensaje de reintento", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: "unavailable" }, 503));

    await expect(listBalances(7)).rejects.toMatchObject({
      type: "server-error",
      message: "No pudimos cargar los saldos. Probá de nuevo.",
    });
  });

  it("traduce un 401 a sesión vencida", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: "Unauthorized" }, 401));

    await expect(listBalances(7)).rejects.toMatchObject({ type: "unauthorized" });
  });
});
