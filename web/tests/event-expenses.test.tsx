import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

vi.mock("@/lib/expenses", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/expenses")>();
  return { ...actual, listExpenses: vi.fn() };
});

import { EventError, type EventDetail } from "@/lib/events";
import { formatMoney, listExpenses, type Expense } from "@/lib/expenses";
import { EventExpenses } from "../components/event-expenses";

const event: EventDetail = {
  id: 7,
  name: "Asado",
  description: null,
  iconKey: "food",
  baseCurrency: "ARS",
  memberCount: 1,
  isOwner: true,
  members: [{ id: 12, name: "Ana", email: "ana@mail.com", isGuest: false }],
};

const cena: Expense = {
  id: 1,
  eventId: 7,
  name: "Cena",
  originalAmount: 10,
  originalCurrency: "ARS",
  baseAmount: 10,
  baseCurrency: "ARS",
  paidByMember: { id: 12, name: "Ana" },
  expenseDate: "2024-03-16",
};

const taxi: Expense = {
  id: 2,
  eventId: 7,
  name: "Taxi",
  originalAmount: 9000,
  originalCurrency: "USD",
  baseAmount: 2.5,
  baseCurrency: "ARS",
  paidByMember: { id: 12, name: "Ana" },
  expenseDate: "2024-03-16",
};

function visibleText(element: Element | null) {
  return (element?.textContent ?? "").replaceAll("\u00a0", " ");
}

function money(amount: number, currency: string) {
  return formatMoney(amount, currency).replaceAll("\u00a0", " ");
}

function renderExpenses() {
  const onUnauthorized = vi.fn();
  const onNotFound = vi.fn();
  render(<EventExpenses event={event} onUnauthorized={onUnauthorized} onNotFound={onNotFound} />);
  return { onUnauthorized, onNotFound };
}

describe("EventExpenses", () => {
  beforeEach(() => {
    vi.mocked(listExpenses).mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("muestra el vacío actual cuando el evento no tiene gastos", async () => {
    vi.mocked(listExpenses).mockResolvedValue([]);

    renderExpenses();

    expect(
      await screen.findByRole("heading", { name: "Todavía no hay gastos" })
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "Cuando cargues gastos, van a aparecer acá para revisar quién pagó y cuánto corresponde."
      )
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Agregar gasto" })).toBeInTheDocument();
    expect(screen.queryByText("Total de gastos")).not.toBeInTheDocument();
  });

  it("muestra nombre, fecha y pagador de cada gasto", async () => {
    vi.mocked(listExpenses).mockResolvedValue([cena]);

    renderExpenses();

    expect(await screen.findByRole("heading", { name: "Cena" })).toBeInTheDocument();
    expect(screen.getByText("16 de mar de 2024")).toBeInTheDocument();
    expect(screen.getByText("Pagó Ana")).toBeInTheDocument();
  });

  it("calcula el total solo con el importe en la moneda base", async () => {
    vi.mocked(listExpenses).mockResolvedValue([cena, taxi]);

    renderExpenses();

    const total = (await screen.findByText("Total de gastos")).closest("article");
    expect(visibleText(total)).toContain(money(12.5, "ARS"));
    expect(visibleText(total)).not.toContain("9.000");
    expect(visibleText(total)).not.toContain("USD");
  });

  it("muestra el importe original solo cuando la moneda es otra", async () => {
    vi.mocked(listExpenses).mockResolvedValue([cena, taxi]);

    renderExpenses();

    const sameCurrency = (await screen.findByRole("heading", { name: "Cena" })).closest("article");
    expect(visibleText(sameCurrency)).toContain(money(10, "ARS"));
    expect(visibleText(sameCurrency)).not.toContain("USD");

    const foreign = screen.getByRole("heading", { name: "Taxi" }).closest("article");
    expect(visibleText(foreign)).toContain(money(2.5, "ARS"));
    expect(visibleText(foreign)).toContain(money(9000, "USD"));
    expect(visibleText(foreign)).not.toContain(`${money(9000, "USD")} USD`);
  });

  it("vuelve a pedir los gastos al reintentar", async () => {
    vi.mocked(listExpenses)
      .mockRejectedValueOnce(new EventError("network", "No pudimos conectar con el servidor."))
      .mockResolvedValueOnce([cena]);

    renderExpenses();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No pudimos conectar con el servidor."
    );

    await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));

    expect(await screen.findByRole("heading", { name: "Cena" })).toBeInTheDocument();
    expect(listExpenses).toHaveBeenCalledTimes(2);
  });

  it("avisa que la sesión venció sin mostrar los gastos", async () => {
    vi.mocked(listExpenses).mockRejectedValue(
      new EventError("unauthorized", "Tu sesión expiró. Iniciá sesión de nuevo.")
    );

    const { onUnauthorized } = renderExpenses();

    await vi.waitFor(() => expect(onUnauthorized).toHaveBeenCalledOnce());
    expect(screen.queryByRole("heading", { name: "Cena" })).not.toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("trata un 403 o un 404 como evento no encontrado", async () => {
    vi.mocked(listExpenses).mockRejectedValueOnce(
      new EventError("forbidden", "No tenés acceso a este evento.")
    );

    const first = renderExpenses();
    await vi.waitFor(() => expect(first.onNotFound).toHaveBeenCalledOnce());
    expect(first.onUnauthorized).not.toHaveBeenCalled();

    cleanup();
    vi.mocked(listExpenses).mockRejectedValueOnce(
      new EventError("not-found", "No encontramos este evento.")
    );

    const second = renderExpenses();
    await vi.waitFor(() => expect(second.onNotFound).toHaveBeenCalledOnce());
  });
});
