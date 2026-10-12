import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

vi.mock("@/lib/balances", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/balances")>();
  return { ...actual, listBalances: vi.fn() };
});

import { EventError, type EventDetail } from "@/lib/events";
import { formatMoney } from "@/lib/expenses";
import { listBalances, type MemberBalance } from "@/lib/balances";
import { EventBalances } from "../components/event-balances";

const event: EventDetail = {
  id: 7,
  name: "Asado",
  description: null,
  iconKey: "food",
  baseCurrency: "ARS",
  memberCount: 3,
  isOwner: true,
  members: [
    { id: 1, name: "Ana", email: "ana@mail.com", isGuest: false },
    { id: 2, name: "Luis", email: null, isGuest: true },
    { id: 3, name: "Sofía", email: null, isGuest: true },
  ],
};

const ana: MemberBalance = { memberId: 1, memberName: "Ana", netBalance: 50 };
const luis: MemberBalance = { memberId: 2, memberName: "Luis", netBalance: -30 };
const sofia: MemberBalance = { memberId: 3, memberName: "Sofía", netBalance: 0 };

function visibleText(element: Element | null) {
  return (element?.textContent ?? "").replaceAll("\u00a0", " ");
}

function money(amount: number, currency: string) {
  return formatMoney(amount, currency).replaceAll("\u00a0", " ");
}

function renderBalances() {
  const onUnauthorized = vi.fn();
  const onNotFound = vi.fn();
  render(<EventBalances event={event} onUnauthorized={onUnauthorized} onNotFound={onNotFound} />);
  return { onUnauthorized, onNotFound };
}

describe("EventBalances", () => {
  beforeEach(() => {
    vi.mocked(listBalances).mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("muestra el vacío cuando todos los saldos están en cero", async () => {
    vi.mocked(listBalances).mockResolvedValue([
      { ...ana, netBalance: 0 },
      { ...luis, netBalance: 0 },
    ]);

    renderBalances();

    expect(
      await screen.findByRole("heading", { name: "Todavía no hay saldos" })
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "Cuando el evento tenga gastos cargados, acá vas a ver cuánto le corresponde a cada integrante."
      )
    ).toBeInTheDocument();
    expect(screen.queryByText("Saldo por integrante")).not.toBeInTheDocument();
  });

  it("muestra si cada integrante recibe, debe o está en cero", async () => {
    vi.mocked(listBalances).mockResolvedValue([ana, luis, sofia]);

    renderBalances();

    expect(
      await screen.findByRole("heading", { name: "Saldo por integrante" })
    ).toBeInTheDocument();

    const receives = screen.getByText("Ana").closest("article");
    expect(visibleText(receives)).toContain("recibe");
    expect(visibleText(receives)).toContain(`+${money(50, "ARS")}`);

    const owes = screen.getByText("Luis").closest("article");
    expect(visibleText(owes)).toContain("debe");
    expect(visibleText(owes)).toContain(money(-30, "ARS"));

    const even = screen.getByText("Sofía").closest("article");
    expect(visibleText(even)).toContain("en cero");
    expect(visibleText(even)).toContain(money(0, "ARS"));
    expect(visibleText(even)).not.toContain("+");
  });

  it("vuelve a pedir los saldos al reintentar", async () => {
    vi.mocked(listBalances)
      .mockRejectedValueOnce(
        new EventError("server-error", "No pudimos cargar los saldos. Probá de nuevo.")
      )
      .mockResolvedValueOnce([ana, luis]);

    renderBalances();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No pudimos cargar los saldos. Probá de nuevo."
    );

    await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));

    expect(await screen.findByText("Ana")).toBeInTheDocument();
    expect(listBalances).toHaveBeenCalledTimes(2);
  });

  it("avisa que la sesión venció sin mostrar saldos", async () => {
    vi.mocked(listBalances).mockRejectedValue(
      new EventError("unauthorized", "Tu sesión expiró. Iniciá sesión de nuevo.")
    );

    const { onUnauthorized } = renderBalances();

    await vi.waitFor(() => expect(onUnauthorized).toHaveBeenCalledOnce());
    expect(screen.queryByText("Ana")).not.toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("trata un 403 o un 404 como evento no encontrado", async () => {
    vi.mocked(listBalances).mockRejectedValueOnce(
      new EventError("forbidden", "No tenés acceso a este evento.")
    );

    const first = renderBalances();
    await vi.waitFor(() => expect(first.onNotFound).toHaveBeenCalledOnce());
    expect(first.onUnauthorized).not.toHaveBeenCalled();

    cleanup();
    vi.mocked(listBalances).mockRejectedValueOnce(
      new EventError("not-found", "No encontramos este evento.")
    );

    const second = renderBalances();
    await vi.waitFor(() => expect(second.onNotFound).toHaveBeenCalledOnce());
  });
});
