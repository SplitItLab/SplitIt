import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { EventError } from "../lib/events";
import {
  createExpense,
  expenseTotal,
  formatExpenseDate,
  formatMoney,
  listExpenses,
  parseExpenseAmount,
  quoteExpense,
  type Expense,
} from "../lib/expenses";

const expense: Expense = {
  id: 1,
  eventId: 7,
  name: "Cena",
  originalAmount: 5000,
  originalCurrency: "ARS",
  baseAmount: 5000,
  baseCurrency: "ARS",
  paidByMember: { id: 12, name: "Ana" },
  expenseDate: "2026-09-24",
};

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("formatMoney, formatExpenseDate y expenseTotal", () => {
  it("formatea el importe en pesos argentinos con decimales", () => {
    expect(formatMoney(69900, "ARS")).toBe("$\u00a069.900,00");
    expect(formatMoney(2.5, "ARS")).toBe("$\u00a02,50");
    expect(formatMoney(10.125, "ARS")).toBe("$\u00a010,125");
  });

  it("lee un monto con punto o con coma y rechaza el resto", () => {
    expect(parseExpenseAmount("10.5")).toBe(10.5);
    expect(parseExpenseAmount("10,5")).toBe(10.5);
    expect(parseExpenseAmount("0")).toBeNull();
    expect(parseExpenseAmount("10,12345")).toBeNull();
    expect(parseExpenseAmount("1.000,50")).toBeNull();
  });

  it("formatea la fecha del gasto en español", () => {
    expect(formatExpenseDate("2024-03-16")).toBe("16 de mar de 2024");
  });

  it("suma solo el importe en la moneda base", () => {
    const expenses: Expense[] = [
      expense,
      {
        ...expense,
        id: 2,
        originalAmount: 9000,
        originalCurrency: "USD",
        baseAmount: 2.5,
        baseCurrency: "ARS",
      },
    ];

    expect(expenseTotal(expenses)).toBe(5002.5);
  });
});

describe("listExpenses, quoteExpense y createExpense", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("pide GET /api/events/:id/expenses y devuelve la lista", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse([expense]));

    await expect(listExpenses(7)).resolves.toEqual([expense]);

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(String(url)).toContain("/api/events/7/expenses");
    expect(String(url)).not.toContain("/quote");
    expect(init?.method ?? "GET").toBe("GET");
  });

  it("pide la cotización con monto y moneda", async () => {
    const quote = {
      originalAmount: 10.5,
      originalCurrency: "USD",
      baseCurrency: "ARS",
      exchangeRate: 1450.5,
      baseAmount: 15230.25,
    };
    vi.mocked(fetch).mockResolvedValue(jsonResponse(quote));

    await expect(quoteExpense(7, 10.5, "USD")).resolves.toEqual(quote);

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    const parsed = new URL(String(url), "http://localhost");
    expect(parsed.pathname).toBe("/api/events/7/expenses/quote");
    expect(parsed.searchParams.get("amount")).toBe("10.5");
    expect(parsed.searchParams.get("currency")).toBe("USD");
    expect(init?.method ?? "GET").toBe("GET");
  });

  it("envía POST /api/events/:id/expenses con el contrato acordado", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(expense, 201));

    const result = await createExpense(7, {
      name: "Cena",
      amount: 5000,
      currency: "ARS",
      paidByMemberId: 12,
    });

    expect(result).toEqual(expense);
    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(String(url)).toContain("/api/events/7/expenses");
    expect(init?.method).toBe("POST");
    expect(JSON.parse(String(init?.body))).toEqual({
      name: "Cena",
      amount: 5000,
      currency: "ARS",
      paidByMemberId: 12,
    });
  });

  it("traduce un 401 a unauthorized", async () => {
    vi.mocked(fetch).mockImplementation(async () => jsonResponse({ message: "Unauthorized" }, 401));

    await expect(listExpenses(7)).rejects.toBeInstanceOf(EventError);
    await expect(listExpenses(7)).rejects.toMatchObject({ type: "unauthorized" });
  });

  it("traduce un 400 a validation", async () => {
    vi.mocked(fetch).mockImplementation(async () =>
      jsonResponse({ message: "Invalid request data" }, 400)
    );

    await expect(
      createExpense(7, {
        name: "Cena",
        amount: 5000,
        currency: "USD",
        paidByMemberId: 12,
      })
    ).rejects.toMatchObject({ type: "validation", message: "Invalid request data" });
  });

  it("traduce un 403 a forbidden", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: "Forbidden" }, 403));

    await expect(listExpenses(7)).rejects.toMatchObject({ type: "forbidden" });
  });

  it("traduce un 404 a not-found", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: "Event not found" }, 404));

    await expect(quoteExpense(7, 10, "USD")).rejects.toMatchObject({ type: "not-found" });
  });

  it("traduce un 503 del listado al mensaje de reintento", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: "unavailable" }, 503));

    await expect(listExpenses(7)).rejects.toMatchObject({
      type: "server-error",
      message: "No pudimos cargar los gastos. Probá de nuevo.",
    });
  });

  it("traduce un 503 de la cotización al mensaje de tipo de cambio", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: "unavailable" }, 503));

    await expect(quoteExpense(7, 10, "USD")).rejects.toMatchObject({
      type: "server-error",
      message: "No pudimos obtener el tipo de cambio.",
    });
  });

  it("traduce un fallo de red a network", async () => {
    vi.mocked(fetch).mockRejectedValue(new TypeError("failed"));

    await expect(listExpenses(7)).rejects.toMatchObject({ type: "network" });
  });
});
