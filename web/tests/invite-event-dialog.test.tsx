import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { InviteEventDialog } from "../components/invite-event-dialog";
import { EventError, type EventDetail } from "../lib/events";

vi.mock("@/lib/events", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/events")>();
  return {
    ...actual,
    getEventInviteToken: vi.fn(),
  };
});

import { getEventInviteToken } from "@/lib/events";

const event: EventDetail = {
  id: 10,
  name: "Viaje a Bariloche",
  description: "Vacaciones",
  iconKey: "plane",
  baseCurrency: "ARS",
  memberCount: 4,
  members: [],
  isOwner: true,
};

const TOKEN = "un-token-ya-persistido-123";

function expectedUrl() {
  return `${window.location.origin}/eventos/invitacion/${TOKEN}`;
}

function renderDialog(onUnauthorized = () => {}) {
  return render(
    <InviteEventDialog open onOpenChange={() => {}} event={event} onUnauthorized={onUnauthorized} />
  );
}

describe("InviteEventDialog", () => {
  const writeText = vi.fn().mockResolvedValue(undefined);

  beforeEach(() => {
    vi.mocked(getEventInviteToken).mockReset();
    writeText.mockReset();
    writeText.mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText },
    });
  });

  it("pide el token al abrir y muestra el enlace con el origen actual", async () => {
    vi.mocked(getEventInviteToken).mockResolvedValue(TOKEN);
    renderDialog();
    const dialog = await screen.findByRole("dialog", { name: "Invitar al evento" });

    expect(await within(dialog).findByDisplayValue(expectedUrl())).toBeInTheDocument();
    expect(getEventInviteToken).toHaveBeenCalledWith(10);
  });

  it("advierte que el enlace no vence ni puede revocarse", async () => {
    vi.mocked(getEventInviteToken).mockResolvedValue(TOKEN);
    renderDialog();
    const dialog = await screen.findByRole("dialog");

    expect(within(dialog).getByText(/Este enlace no vence ni puede revocarse/)).toBeInTheDocument();
  });

  it("muestra estado de carga mientras pide el token", async () => {
    vi.mocked(getEventInviteToken).mockReturnValue(new Promise(() => {}));
    renderDialog();
    const dialog = await screen.findByRole("dialog");

    expect(within(dialog).getByText("Generando enlace…")).toBeInTheDocument();
  });

  it("copia la URL completa y muestra una confirmación", async () => {
    vi.mocked(getEventInviteToken).mockResolvedValue(TOKEN);
    const user = userEvent.setup();
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText },
    });
    renderDialog();
    const dialog = await screen.findByRole("dialog");
    await within(dialog).findByDisplayValue(expectedUrl());

    await user.click(within(dialog).getByRole("button", { name: "Copiar enlace" }));

    expect(writeText).toHaveBeenCalledWith(expectedUrl());
    expect(
      await within(dialog).findByRole("button", { name: "Enlace copiado" })
    ).toBeInTheDocument();
    expect(within(dialog).queryByRole("status")).not.toBeInTheDocument();
  });

  it("muestra un error y permite reintentar si la API falla", async () => {
    vi.mocked(getEventInviteToken)
      .mockRejectedValueOnce(new EventError("network", "No pudimos conectar con el servidor."))
      .mockResolvedValueOnce(TOKEN);
    const user = userEvent.setup();
    renderDialog();
    const dialog = await screen.findByRole("dialog");

    expect(
      await within(dialog).findByText("No pudimos conectar con el servidor.")
    ).toBeInTheDocument();

    await user.click(within(dialog).getByRole("button", { name: "Reintentar" }));

    expect(await within(dialog).findByDisplayValue(expectedUrl())).toBeInTheDocument();
    expect(getEventInviteToken).toHaveBeenCalledTimes(2);
  });

  it("muestra el error 403 en el modal", async () => {
    vi.mocked(getEventInviteToken).mockRejectedValue(
      new EventError("forbidden", "Solo el dueño puede invitar a este evento.")
    );
    renderDialog();
    const dialog = await screen.findByRole("dialog");

    expect(
      await within(dialog).findByText("Solo el dueño puede invitar a este evento.")
    ).toBeInTheDocument();
    expect(within(dialog).getByRole("button", { name: "Reintentar" })).toBeInTheDocument();
  });

  it("muestra el error 404 en el modal", async () => {
    vi.mocked(getEventInviteToken).mockRejectedValue(
      new EventError("not-found", "No encontramos este evento.")
    );
    renderDialog();
    const dialog = await screen.findByRole("dialog");

    expect(await within(dialog).findByText("No encontramos este evento.")).toBeInTheDocument();
  });

  it("redirige por sesión vencida y no muestra error en el modal", async () => {
    const onUnauthorized = vi.fn();
    vi.mocked(getEventInviteToken).mockRejectedValue(
      new EventError("unauthorized", "Tu sesión expiró. Iniciá sesión de nuevo.")
    );
    renderDialog(onUnauthorized);
    const dialog = await screen.findByRole("dialog");

    await waitFor(() => expect(onUnauthorized).toHaveBeenCalledTimes(1));
    expect(within(dialog).queryByRole("alert")).not.toBeInTheDocument();
  });
});
