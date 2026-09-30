import { type ComponentProps } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

vi.mock("@/lib/expenses", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/expenses")>();
  return {
    ...actual,
    listExpenses: vi.fn(),
    quoteExpense: vi.fn(),
    createExpense: vi.fn(),
  };
});

vi.mock("@/lib/toast", () => ({
  showAppToast: vi.fn(),
}));

import { EventError, type EventDetail } from "@/lib/events";
import {
  createExpense,
  formatMoney,
  listExpenses,
  quoteExpense,
  type Expense,
} from "@/lib/expenses";
import { showAppToast } from "@/lib/toast";
import { AddExpenseDialog } from "../components/add-expense-dialog";
import { EventExpenses } from "../components/event-expenses";

const event: EventDetail = {
  id: 7,
  name: "Asado",
  baseCurrency: "ARS",
  memberCount: 2,
  isOwner: false,
  members: [
    { id: 12, name: "Ana", email: "ana@mail.com", isGuest: false },
    { id: 13, name: "Luis", email: null, isGuest: true },
  ],
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

const created: Expense = {
  id: 9,
  eventId: 7,
  name: "Supermercado",
  originalAmount: 5,
  originalCurrency: "ARS",
  baseAmount: 5,
  baseCurrency: "ARS",
  paidByMember: { id: 12, name: "Ana" },
  expenseDate: "2026-09-30",
};

function visibleText(element: Element | null) {
  return (element?.textContent ?? "").replaceAll("\u00a0", " ");
}

function renderDialog(
  props: Partial<ComponentProps<typeof AddExpenseDialog>> = {},
  current: EventDetail = event
) {
  const onUnauthorized = vi.fn();
  const onNotFound = vi.fn();
  const onCreated = vi.fn();
  const onOpenChange = vi.fn();
  render(
    <AddExpenseDialog
      open
      onOpenChange={onOpenChange}
      event={current}
      onCreated={onCreated}
      onUnauthorized={onUnauthorized}
      onNotFound={onNotFound}
      {...props}
    />
  );
  return { onUnauthorized, onNotFound, onCreated, onOpenChange };
}

describe("AddExpenseDialog", () => {
  beforeEach(() => {
    vi.mocked(quoteExpense).mockReset();
    vi.mocked(createExpense).mockReset();
    vi.mocked(listExpenses).mockReset();
    vi.mocked(showAppToast).mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("empieza en la moneda base y no pide cotización", async () => {
    const user = userEvent.setup();
    renderDialog();

    expect(await screen.findByRole("dialog", { name: "Agregar gasto" })).toBeInTheDocument();
    expect(screen.getByLabelText("Moneda")).toHaveValue("ARS");
    await user.type(screen.getByLabelText("Monto"), "15");
    await new Promise((resolve) => setTimeout(resolve, 400));

    expect(quoteExpense).not.toHaveBeenCalled();
    expect(screen.queryByText("Obteniendo tipo de cambio…")).not.toBeInTheDocument();
  });

  it("muestra la tasa y el importe convertido, y no deja guardar mientras cotiza", async () => {
    let resolveQuote: (quote: {
      originalAmount: number;
      originalCurrency: string;
      baseCurrency: string;
      exchangeRate: number;
      baseAmount: number;
    }) => void = () => {};
    vi.mocked(quoteExpense).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveQuote = resolve;
        })
    );
    const user = userEvent.setup();
    renderDialog();

    await user.type(screen.getByLabelText("Monto"), "10");
    await user.selectOptions(screen.getByLabelText("Moneda"), "USD");

    expect(screen.getByText("Obteniendo tipo de cambio…")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Guardar gasto" })).toBeDisabled();

    await waitFor(() =>
      expect(quoteExpense).toHaveBeenCalledWith(
        7,
        10,
        "USD",
        expect.objectContaining({ signal: expect.any(AbortSignal) })
      )
    );
    resolveQuote({
      originalAmount: 10,
      originalCurrency: "USD",
      baseCurrency: "ARS",
      exchangeRate: 1450.5,
      baseAmount: 14505,
    });

    const rate = (1450.5).toLocaleString("es-AR", { maximumFractionDigits: 4 });
    expect(await screen.findByText(`1 USD = ${rate} ARS`)).toBeInTheDocument();
    expect(screen.getByText(/Se guardará como/)).toHaveTextContent(/Se guardará como/);
    expect(screen.getByRole("button", { name: "Guardar gasto" })).toBeEnabled();
  });

  it("acepta la coma decimal y cotiza ese monto", async () => {
    vi.mocked(quoteExpense).mockResolvedValue({
      originalAmount: 10.5,
      originalCurrency: "USD",
      baseCurrency: "ARS",
      exchangeRate: 1450.5,
      baseAmount: 15230.25,
    });
    const user = userEvent.setup();
    renderDialog();

    await user.type(screen.getByLabelText("Monto"), "10,5");
    await user.selectOptions(screen.getByLabelText("Moneda"), "USD");

    await waitFor(() =>
      expect(quoteExpense).toHaveBeenCalledWith(
        7,
        10.5,
        "USD",
        expect.objectContaining({ signal: expect.any(AbortSignal) })
      )
    );
  });

  it("ofrece reintentar la cotización y mantiene el guardado deshabilitado", async () => {
    vi.mocked(quoteExpense).mockRejectedValue(new EventError("server-error", "no"));
    const user = userEvent.setup();
    renderDialog();

    await user.type(screen.getByLabelText("Monto"), "10");
    await user.selectOptions(screen.getByLabelText("Moneda"), "USD");

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No pudimos obtener el tipo de cambio."
    );
    expect(screen.getByRole("button", { name: "Guardar gasto" })).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Reintentar" }));

    await waitFor(() => expect(quoteExpense).toHaveBeenCalledTimes(2));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No pudimos obtener el tipo de cambio."
    );
    expect(screen.getByRole("button", { name: "Guardar gasto" })).toBeDisabled();
  });

  it("no guarda con nombre vacío, monto inválido o sin pagador", async () => {
    const user = userEvent.setup();
    const { onCreated } = renderDialog();

    await user.click(screen.getByRole("button", { name: "Guardar gasto" }));
    expect(screen.getByRole("alert")).toHaveTextContent("Ingresá un nombre para el gasto");

    await user.type(screen.getByLabelText("Nombre"), "Supermercado");
    await user.type(screen.getByLabelText("Monto"), "0");
    await user.click(screen.getByRole("button", { name: "Guardar gasto" }));
    expect(screen.getByRole("alert")).toHaveTextContent("Ingresá un monto válido");

    cleanup();
    renderDialog({}, { ...event, members: [] });
    await user.type(screen.getByLabelText("Nombre"), "Supermercado");
    await user.type(screen.getByLabelText("Monto"), "10");
    await user.click(screen.getByRole("button", { name: "Guardar gasto" }));

    expect(screen.getByRole("alert")).toHaveTextContent("Seleccioná quién pagó");
    expect(createExpense).not.toHaveBeenCalled();
    expect(onCreated).not.toHaveBeenCalled();
  });

  it("ofrece como pagador solo a los integrantes del evento", async () => {
    renderDialog();

    const payer = await screen.findByLabelText("Pago");
    expect(
      within(payer)
        .getAllByRole("option")
        .map((option) => option.textContent)
    ).toEqual(["Ana", "Luis"]);
  });

  it("antepone el gasto creado, actualiza el total y confirma", async () => {
    vi.mocked(listExpenses).mockResolvedValue([cena]);
    vi.mocked(createExpense).mockResolvedValue(created);
    const user = userEvent.setup();
    render(<EventExpenses event={event} onUnauthorized={vi.fn()} onNotFound={vi.fn()} />);

    await user.click(await screen.findByRole("button", { name: "Agregar gasto" }));
    const dialog = await screen.findByRole("dialog", { name: "Agregar gasto" });
    await user.type(within(dialog).getByLabelText("Nombre"), "Supermercado");
    await user.type(within(dialog).getByLabelText("Monto"), "5");
    await user.click(within(dialog).getByRole("button", { name: "Guardar gasto" }));

    expect(createExpense).toHaveBeenCalledWith(7, {
      name: "Supermercado",
      amount: 5,
      currency: "ARS",
      paidByMemberId: 12,
    });
    const names = await screen.findAllByRole("heading", { level: 3 });
    expect(names.map((heading) => heading.textContent)).toEqual(["Supermercado", "Cena"]);
    const total = screen.getByText("Total de gastos").closest("article");
    expect(visibleText(total)).toContain(visibleTextOf(formatMoney(15, "ARS")));
    expect(showAppToast).toHaveBeenCalledWith("success", "Agregamos «Supermercado»");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(listExpenses).toHaveBeenCalledTimes(1);
  });

  it("avisa que la sesión venció al guardar", async () => {
    vi.mocked(createExpense).mockRejectedValue(new EventError("unauthorized", "Tu sesión expiró."));
    const user = userEvent.setup();
    const { onUnauthorized } = renderDialog();

    await user.type(screen.getByLabelText("Nombre"), "Supermercado");
    await user.type(screen.getByLabelText("Monto"), "5");
    await user.click(screen.getByRole("button", { name: "Guardar gasto" }));

    await waitFor(() => expect(onUnauthorized).toHaveBeenCalledOnce());
  });

  it("deja el diálogo abierto cuando el alta responde 400", async () => {
    vi.mocked(createExpense).mockRejectedValue(
      new EventError("validation", "Invalid request data")
    );
    const user = userEvent.setup();
    renderDialog();

    await user.type(screen.getByLabelText("Nombre"), "Supermercado");
    await user.type(screen.getByLabelText("Monto"), "5");
    await user.click(screen.getByRole("button", { name: "Guardar gasto" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid request data");
    expect(screen.getByRole("dialog", { name: "Agregar gasto" })).toBeInTheDocument();
  });

  it("se recorre con teclado y se cierra con Escape", async () => {
    vi.mocked(listExpenses).mockResolvedValue([]);
    const user = userEvent.setup();
    render(<EventExpenses event={event} onUnauthorized={vi.fn()} onNotFound={vi.fn()} />);

    await user.click(await screen.findByRole("button", { name: "Agregar gasto" }));
    const dialog = await screen.findByRole("dialog", { name: "Agregar gasto" });

    await waitFor(() => {
      expect(dialog === document.activeElement || dialog.contains(document.activeElement)).toBe(
        true
      );
    });
    expect(within(dialog).getByLabelText("Nombre")).toBe(screen.getByLabelText("Nombre"));
    expect(within(dialog).getByLabelText("Monto")).toHaveAttribute("id", "expense-amount");
    expect(within(dialog).getByLabelText("Moneda")).toHaveAttribute("id", "expense-currency");
    expect(within(dialog).getByLabelText("Pago")).toHaveAttribute("id", "expense-payer");

    await user.tab();
    expect(dialog === document.activeElement || dialog.contains(document.activeElement)).toBe(true);
    await user.keyboard("{Escape}");

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  });
});

function visibleTextOf(value: string) {
  return value.replaceAll("\u00a0", " ");
}
