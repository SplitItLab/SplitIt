import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { act, cleanup, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

vi.mock("@/lib/expenses", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/expenses")>();
  return { ...actual, listExpenses: vi.fn() };
});

vi.mock("@/lib/settlements", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/settlements")>();
  return { ...actual, listSuggestedPayments: vi.fn() };
});

import { EventError, type EventDetail } from "@/lib/events";
import { formatMoney, listExpenses } from "@/lib/expenses";
import { listSuggestedPayments, type SuggestedPayment } from "@/lib/settlements";
import { EventSuggestedPayments } from "../components/event-suggested-payments";

const event: EventDetail = {
  id: 7,
  name: "Asado",
  description: null,
  iconKey: "food",
  baseCurrency: "ARS",
  memberCount: 3,
  isOwner: true,
  members: [
    { id: 12, name: "Ana", email: "ana@mail.com", isGuest: false },
    { id: 13, name: "Juan", isGuest: true },
    { id: 14, name: "Sofi", isGuest: true },
  ],
};

const juanPagaAAna: SuggestedPayment = {
  fromMember: { id: 13, name: "Juan" },
  toMember: { id: 12, name: "Ana" },
  amount: 2500,
};

const sofiPagaAAna: SuggestedPayment = {
  fromMember: { id: 14, name: "Sofi" },
  toMember: { id: 12, name: "Ana" },
  amount: 2500.5,
};

function visibleText(element: Element | null) {
  return (element?.textContent ?? "").replaceAll(" ", " ");
}

function money(amount: number, currency: string) {
  return formatMoney(amount, currency).replaceAll(" ", " ");
}

function renderSuggestedPayments(hasExpenses: boolean | null = true) {
  const onUnauthorized = vi.fn();
  const onNotFound = vi.fn();
  const view = render(
    <EventSuggestedPayments
      event={event}
      hasExpenses={hasExpenses}
      onUnauthorized={onUnauthorized}
      onNotFound={onNotFound}
    />
  );
  return { ...view, onUnauthorized, onNotFound };
}

describe("EventSuggestedPayments", () => {
  beforeEach(() => {
    vi.mocked(listExpenses).mockReset();
    vi.mocked(listSuggestedPayments).mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("muestra quién paga, quién recibe y el monto de cada pago sugerido", async () => {
    vi.mocked(listSuggestedPayments).mockResolvedValue([juanPagaAAna, sofiPagaAAna]);

    renderSuggestedPayments();

    expect(await screen.findByRole("heading", { name: "Pagos sugeridos" })).toBeInTheDocument();

    const juan = screen.getByRole("article", { name: "Juan le paga a Ana" });
    expect(within(juan).getByText("debe pagar")).toBeInTheDocument();
    expect(within(juan).getByText("debe recibir")).toBeInTheDocument();
    expect(visibleText(juan)).toContain(money(2500, "ARS"));

    const sofi = screen.getByRole("article", { name: "Sofi le paga a Ana" });
    expect(visibleText(sofi)).toContain(money(2500.5, "ARS"));

    expect(listSuggestedPayments).toHaveBeenCalledWith(7);
  });

  it("muestra que el split está saldado cuando hay gastos pero no deudas pendientes", async () => {
    vi.mocked(listSuggestedPayments).mockResolvedValue([]);

    renderSuggestedPayments();

    expect(await screen.findByText("Está todo claro")).toBeInTheDocument();
    expect(screen.getByText("No hay pagos pendientes.")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Pagos sugeridos" })).toBeInTheDocument();
    expect(screen.queryByRole("article")).not.toBeInTheDocument();
  });

  it("muestra un estado vacío cuando el evento no tiene gastos", async () => {
    vi.mocked(listSuggestedPayments).mockResolvedValue([]);

    renderSuggestedPayments(false);

    expect(await screen.findByText("Todavía no hay pagos sugeridos")).toBeInTheDocument();
    expect(
      screen.getByText(
        "Cuando el evento tenga gastos cargados, acá vas a ver quién le tiene que pagar a quién."
      )
    ).toBeInTheDocument();
    expect(screen.queryByText("Está todo claro")).not.toBeInTheDocument();
  });

  it("avisa la sesión vencida", async () => {
    vi.mocked(listSuggestedPayments).mockRejectedValue(
      new EventError("unauthorized", "Tu sesión expiró.")
    );

    const { onUnauthorized, onNotFound } = renderSuggestedPayments();

    await vi.waitFor(() => expect(onUnauthorized).toHaveBeenCalled());
    expect(onNotFound).not.toHaveBeenCalled();
  });

  it("trata un evento sin acceso como no encontrado", async () => {
    vi.mocked(listSuggestedPayments).mockRejectedValue(
      new EventError("forbidden", "No tenés acceso a este evento.")
    );

    const { onUnauthorized, onNotFound } = renderSuggestedPayments();

    await vi.waitFor(() => expect(onNotFound).toHaveBeenCalled());
    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it("muestra el error y permite reintentar", async () => {
    vi.mocked(listSuggestedPayments)
      .mockRejectedValueOnce(
        new EventError("server-error", "No pudimos cargar los pagos sugeridos. Probá de nuevo.")
      )
      .mockResolvedValueOnce([juanPagaAAna]);

    renderSuggestedPayments();

    expect(
      await screen.findByText("No pudimos cargar los pagos sugeridos. Probá de nuevo.")
    ).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));

    expect(await screen.findByRole("article", { name: "Juan le paga a Ana" })).toBeInTheDocument();
  });

  it("muestra los pagos sugeridos aunque falle la consulta de gastos", async () => {
    vi.mocked(listExpenses).mockRejectedValue(new EventError("network", "Error de gastos."));
    vi.mocked(listSuggestedPayments).mockResolvedValue([juanPagaAAna]);

    renderSuggestedPayments(null);

    expect(await screen.findByRole("article", { name: "Juan le paga a Ana" })).toBeInTheDocument();
    expect(listExpenses).not.toHaveBeenCalled();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it.each(["unauthorized", "forbidden", "not-found"] as const)(
    "ignora un error %s después de desmontarse",
    async (type) => {
      const pending = Promise.withResolvers<SuggestedPayment[]>();
      vi.mocked(listSuggestedPayments).mockReturnValue(pending.promise);

      const { unmount, onUnauthorized, onNotFound } = renderSuggestedPayments();
      unmount();

      await act(async () => {
        pending.reject(new EventError(type, "Respuesta tardía."));
      });

      expect(onUnauthorized).not.toHaveBeenCalled();
      expect(onNotFound).not.toHaveBeenCalled();
    }
  );

  it("ignora los pagos de una solicitud anterior cuando cambia el evento", async () => {
    const pending = Promise.withResolvers<SuggestedPayment[]>();
    vi.mocked(listSuggestedPayments)
      .mockReturnValueOnce(pending.promise)
      .mockResolvedValueOnce([sofiPagaAAna]);
    const { rerender, onUnauthorized, onNotFound } = renderSuggestedPayments();

    rerender(
      <EventSuggestedPayments
        event={{ ...event, id: 8 }}
        hasExpenses
        onUnauthorized={onUnauthorized}
        onNotFound={onNotFound}
      />
    );
    await screen.findByRole("article", { name: "Sofi le paga a Ana" });

    await act(async () => {
      pending.resolve([juanPagaAAna]);
    });

    expect(screen.getByRole("article", { name: "Sofi le paga a Ana" })).toBeInTheDocument();
    expect(screen.queryByRole("article", { name: "Juan le paga a Ana" })).not.toBeInTheDocument();
  });
});
