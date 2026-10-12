import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

vi.mock("@/lib/expenses", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/expenses")>();
  return {
    ...actual,
    listExpenses: vi.fn(),
    deleteExpense: vi.fn(),
  };
});

vi.mock("@/lib/toast", () => ({
  showAppToast: vi.fn(),
}));

import { EventError, type EventDetail } from "@/lib/events";
import { deleteExpense, formatMoney, listExpenses, type Expense } from "@/lib/expenses";
import { showAppToast } from "@/lib/toast";
import { DeleteExpenseDialog } from "../components/delete-expense-dialog";
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

const carne: Expense = {
  id: 1,
  eventId: 7,
  name: "Carne y achuras",
  originalAmount: 32000,
  originalCurrency: "ARS",
  baseAmount: 32000,
  baseCurrency: "ARS",
  paidByMember: { id: 12, name: "Ana" },
  expenseDate: "2024-03-16",
};

const bebidas: Expense = {
  ...carne,
  id: 2,
  name: "Bebidas",
  originalAmount: 14500,
  baseAmount: 14500,
};

function visibleText(element: Element | null) {
  return (element?.textContent ?? "").replaceAll(" ", " ");
}

function money(amount: number, currency: string) {
  return formatMoney(amount, currency).replaceAll(" ", " ");
}

function renderDialog(overrides: Partial<Parameters<typeof DeleteExpenseDialog>[0]> = {}) {
  const props = {
    open: true,
    onOpenChange: vi.fn(),
    expense: carne,
    onDeleted: vi.fn(),
    onAlreadyDeleted: vi.fn(),
    onUnauthorized: vi.fn(),
    onNotFound: vi.fn(),
    ...overrides,
  };
  render(<DeleteExpenseDialog {...props} />);
  return props;
}

beforeEach(() => {
  vi.mocked(deleteExpense).mockReset();
  vi.mocked(listExpenses).mockReset();
  vi.mocked(showAppToast).mockReset();
});

afterEach(() => {
  cleanup();
});

describe("DeleteExpenseDialog", () => {
  it("pide confirmación mostrando el nombre del gasto", async () => {
    renderDialog();
    const dialog = await screen.findByRole("dialog");

    expect(
      within(dialog).getByText("¿Estás seguro que querés eliminar este gasto?")
    ).toBeInTheDocument();
    expect(within(dialog).getByText(/Vas a eliminar Carne y achuras/)).toBeInTheDocument();
  });

  it("cancelar cierra sin eliminar nada", async () => {
    const user = userEvent.setup();
    const { onOpenChange, onDeleted } = renderDialog();
    const dialog = await screen.findByRole("dialog");

    await user.click(within(dialog).getByRole("button", { name: "Cancelar" }));

    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(deleteExpense).not.toHaveBeenCalled();
    expect(onDeleted).not.toHaveBeenCalled();
  });

  it("confirmar envía una sola request aunque se haga doble click", async () => {
    let resolveDelete!: (value: void) => void;
    vi.mocked(deleteExpense).mockReturnValue(
      new Promise<void>((resolve) => {
        resolveDelete = resolve;
      })
    );
    const user = userEvent.setup();
    const { onDeleted } = renderDialog();
    const dialog = await screen.findByRole("dialog");

    const confirm = within(dialog).getByRole("button", { name: "Eliminar" });
    await user.click(confirm);
    await waitFor(() => expect(confirm).toBeDisabled());
    await user.click(confirm);

    expect(deleteExpense).toHaveBeenCalledTimes(1);
    expect(deleteExpense).toHaveBeenCalledWith(7, 1);

    resolveDelete();
    await waitFor(() => expect(onDeleted).toHaveBeenCalledWith(carne));
  });

  it("muestra un error claro si falla la eliminación", async () => {
    vi.mocked(deleteExpense).mockRejectedValue(
      new EventError("server-error", "No pudimos eliminar el gasto. Probá de nuevo.")
    );
    const user = userEvent.setup();
    const { onDeleted } = renderDialog();
    const dialog = await screen.findByRole("dialog");

    await user.click(within(dialog).getByRole("button", { name: "Eliminar" }));

    expect(
      await within(dialog).findByText("No pudimos eliminar el gasto. Probá de nuevo.")
    ).toBeInTheDocument();
    expect(onDeleted).not.toHaveBeenCalled();
  });

  it("avisa que la sesión venció", async () => {
    vi.mocked(deleteExpense).mockRejectedValue(
      new EventError("unauthorized", "Tu sesión expiró. Iniciá sesión de nuevo.")
    );
    const user = userEvent.setup();
    const { onUnauthorized, onDeleted } = renderDialog();
    const dialog = await screen.findByRole("dialog");

    await user.click(within(dialog).getByRole("button", { name: "Eliminar" }));

    await waitFor(() => expect(onUnauthorized).toHaveBeenCalledOnce());
    expect(onDeleted).not.toHaveBeenCalled();
  });

  it("trata un 403 como evento no encontrado", async () => {
    vi.mocked(deleteExpense).mockRejectedValue(
      new EventError("forbidden", "No tenés acceso a este evento.")
    );
    const user = userEvent.setup();
    const { onNotFound, onAlreadyDeleted, onDeleted } = renderDialog();
    const dialog = await screen.findByRole("dialog");

    await user.click(within(dialog).getByRole("button", { name: "Eliminar" }));

    await waitFor(() => expect(onNotFound).toHaveBeenCalledOnce());
    expect(onAlreadyDeleted).not.toHaveBeenCalled();
    expect(onDeleted).not.toHaveBeenCalled();
  });

  it("trata un 404 como gasto ya eliminado, sin dar el evento por perdido", async () => {
    vi.mocked(deleteExpense).mockRejectedValue(
      new EventError("not-found", "No encontramos este evento.")
    );
    const user = userEvent.setup();
    const { onNotFound, onAlreadyDeleted, onDeleted } = renderDialog();
    const dialog = await screen.findByRole("dialog");

    await user.click(within(dialog).getByRole("button", { name: "Eliminar" }));

    await waitFor(() => expect(onAlreadyDeleted).toHaveBeenCalledWith(carne));
    expect(onNotFound).not.toHaveBeenCalled();
    expect(onDeleted).not.toHaveBeenCalled();
  });
});

describe("EventExpenses — eliminar gasto", () => {
  function renderExpenses() {
    const onNotFound = vi.fn();
    render(<EventExpenses event={event} onUnauthorized={vi.fn()} onNotFound={onNotFound} />);
    return { onNotFound };
  }

  it("cancelar deja el gasto en la lista y el total igual", async () => {
    vi.mocked(listExpenses).mockResolvedValue([carne, bebidas]);
    const user = userEvent.setup();
    renderExpenses();

    await user.click(await screen.findByRole("button", { name: "Eliminar gasto Carne y achuras" }));
    const dialog = await screen.findByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "Cancelar" }));

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(screen.getByRole("heading", { name: "Carne y achuras" })).toBeInTheDocument();
    const total = screen.getByText("Total de gastos").closest("article");
    expect(visibleText(total)).toContain(money(46500, "ARS"));
    expect(deleteExpense).not.toHaveBeenCalled();
  });

  it("confirmar saca el gasto de la lista, recalcula el total y avisa sin recargar", async () => {
    vi.mocked(listExpenses).mockResolvedValue([carne, bebidas]);
    vi.mocked(deleteExpense).mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderExpenses();

    await user.click(await screen.findByRole("button", { name: "Eliminar gasto Carne y achuras" }));
    const dialog = await screen.findByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "Eliminar" }));

    await waitFor(() =>
      expect(screen.queryByRole("heading", { name: "Carne y achuras" })).not.toBeInTheDocument()
    );
    expect(screen.getByRole("heading", { name: "Bebidas" })).toBeInTheDocument();
    const total = screen.getByText("Total de gastos").closest("article");
    expect(visibleText(total)).toContain(money(14500, "ARS"));
    expect(showAppToast).toHaveBeenCalledWith("success", "Eliminamos «Carne y achuras»");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(deleteExpense).toHaveBeenCalledWith(7, 1);
    expect(listExpenses).toHaveBeenCalledTimes(1);
  });

  it("si el gasto ya fue eliminado en otra pestaña, actualiza la lista y mantiene el evento", async () => {
    vi.mocked(listExpenses)
      .mockResolvedValueOnce([carne, bebidas])
      .mockResolvedValueOnce([bebidas]);
    vi.mocked(deleteExpense).mockRejectedValue(
      new EventError("not-found", "No encontramos este evento.")
    );
    const user = userEvent.setup();
    const { onNotFound } = renderExpenses();

    await user.click(await screen.findByRole("button", { name: "Eliminar gasto Carne y achuras" }));
    const dialog = await screen.findByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "Eliminar" }));

    await waitFor(() =>
      expect(screen.queryByRole("heading", { name: "Carne y achuras" })).not.toBeInTheDocument()
    );
    expect(screen.getByRole("heading", { name: "Bebidas" })).toBeInTheDocument();
    const total = screen.getByText("Total de gastos").closest("article");
    expect(visibleText(total)).toContain(money(14500, "ARS"));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(showAppToast).toHaveBeenCalledWith("info", "«Carne y achuras» ya había sido eliminado");
    expect(listExpenses).toHaveBeenCalledTimes(2);
    expect(onNotFound).not.toHaveBeenCalled();
  });

  it("si lo que ya no existe es el evento, lo trata como evento no encontrado", async () => {
    vi.mocked(listExpenses)
      .mockResolvedValueOnce([carne, bebidas])
      .mockRejectedValueOnce(new EventError("not-found", "No encontramos este evento."));
    vi.mocked(deleteExpense).mockRejectedValue(
      new EventError("not-found", "No encontramos este evento.")
    );
    const user = userEvent.setup();
    const { onNotFound } = renderExpenses();

    await user.click(await screen.findByRole("button", { name: "Eliminar gasto Carne y achuras" }));
    const dialog = await screen.findByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "Eliminar" }));

    await waitFor(() => expect(onNotFound).toHaveBeenCalledOnce());
  });
});
