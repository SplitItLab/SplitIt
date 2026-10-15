import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { EventError } from "../lib/events";
import { listSuggestedPayments, type SuggestedPayment } from "../lib/settlements";

const payment: SuggestedPayment = {
  fromMember: { id: 13, name: "Juan" },
  toMember: { id: 12, name: "Ana" },
  amount: 2500,
};

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("listSuggestedPayments", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("pide GET /api/events/:id/suggested-payments y devuelve la lista", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse([payment]));

    await expect(listSuggestedPayments(7)).resolves.toEqual([payment]);

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(String(url)).toContain("/api/events/7/suggested-payments");
    expect(init?.method ?? "GET").toBe("GET");
  });

  it("traduce 401 a un error de sesión", async () => {
    vi.mocked(fetch).mockImplementation(async () => jsonResponse({ message: "Unauthorized" }, 401));

    await expect(listSuggestedPayments(7)).rejects.toBeInstanceOf(EventError);
    await expect(listSuggestedPayments(7)).rejects.toMatchObject({ type: "unauthorized" });
  });

  it("traduce 403 y 404 a errores de acceso al evento", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse({ message: "Forbidden" }, 403));
    await expect(listSuggestedPayments(7)).rejects.toMatchObject({ type: "forbidden" });

    vi.mocked(fetch).mockResolvedValueOnce(jsonResponse({ message: "Event not found" }, 404));
    await expect(listSuggestedPayments(7)).rejects.toMatchObject({ type: "not-found" });
  });

  it("usa un mensaje propio cuando falla el servidor", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: "boom" }, 500));

    await expect(listSuggestedPayments(7)).rejects.toMatchObject({
      type: "server-error",
      message: "No pudimos cargar los pagos sugeridos. Probá de nuevo.",
    });
  });
});
