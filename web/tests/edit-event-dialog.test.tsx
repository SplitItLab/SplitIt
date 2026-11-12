import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { EditEventDialog } from "../components/edit-event-dialog";
import { EventError, type EventDetail, type EventSummary } from "../lib/events";

vi.mock("@/lib/events", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/events")>();
  return {
    ...actual,
    updateEvent: vi.fn(),
  };
});

import { updateEvent } from "@/lib/events";

const event: EventDetail = {
  id: 1,
  name: "Viaje a Bariloche",
  description: "Vacaciones de verano",
  iconKey: "plane",
  baseCurrency: "ARS",
  memberCount: 4,
  members: [],
  isOwner: true,
};

function renderDialog() {
  return render(
    <EditEventDialog
      open
      onOpenChange={() => {}}
      event={event}
      onUpdated={() => {}}
      onUnauthorized={() => {}}
    />
  );
}

describe("EditEventDialog", () => {
  beforeEach(() => {
    vi.mocked(updateEvent).mockReset();
  });

  it("precarga los valores actuales y muestra la moneda como solo lectura", async () => {
    renderDialog();
    const dialog = await screen.findByRole("dialog");

    expect(within(dialog).getByLabelText("Nombre del evento")).toHaveValue("Viaje a Bariloche");
    expect(within(dialog).getByLabelText(/Descripción/)).toHaveValue("Vacaciones de verano");
    expect(within(dialog).getByLabelText("Moneda del evento")).toHaveTextContent(
      "ARS - Peso argentino"
    );
    expect(within(dialog).queryByRole("combobox")).not.toBeInTheDocument();
  });

  it("no permite requests duplicadas mientras guarda", async () => {
    let resolveUpdate!: (value: EventSummary) => void;
    vi.mocked(updateEvent).mockReturnValue(
      new Promise<EventSummary>((resolve) => {
        resolveUpdate = resolve;
      })
    );
    const user = userEvent.setup();
    renderDialog();
    const dialog = await screen.findByRole("dialog");

    await user.clear(within(dialog).getByLabelText("Nombre del evento"));
    await user.type(within(dialog).getByLabelText("Nombre del evento"), "Viaje a Mendoza");

    const submit = within(dialog).getByRole("button", { name: /Guardar cambios/ });
    await user.click(submit);
    await waitFor(() => expect(submit).toBeDisabled());
    await user.click(submit);

    expect(updateEvent).toHaveBeenCalledTimes(1);
    resolveUpdate({ ...event, name: "Viaje a Mendoza" });
    await waitFor(() =>
      expect(vi.mocked(updateEvent).mock.calls[0]).toEqual([
        1,
        expect.objectContaining({ name: "Viaje a Mendoza" }),
      ])
    );
  });

  // SPT-119: la descripción tiene un máximo de 500 caracteres, validado antes de enviar.
  it("no guarda cuando la descripción supera el máximo", async () => {
    const user = userEvent.setup();
    renderDialog();
    const dialog = await screen.findByRole("dialog");

    const description = within(dialog).getByLabelText(/Descripción/);
    await user.clear(description);
    await user.paste("X".repeat(501));
    await user.click(within(dialog).getByRole("button", { name: /Guardar cambios/ }));

    expect(
      await within(dialog).findByText("La descripción puede tener hasta 500 caracteres.")
    ).toBeInTheDocument();
    expect(updateEvent).not.toHaveBeenCalled();
  });

  // SPT-119: un evento guardado antes del límite puede tener más de 500 caracteres de descripción.
  it("permite editar el nombre sin tocar una descripción guardada más larga que el máximo", async () => {
    const longDescription = "X".repeat(600);
    const eventWithLongDescription: EventDetail = { ...event, description: longDescription };
    vi.mocked(updateEvent).mockResolvedValue({
      ...eventWithLongDescription,
      name: "Viaje a Mendoza",
    });
    const user = userEvent.setup();
    render(
      <EditEventDialog
        open
        onOpenChange={() => {}}
        event={eventWithLongDescription}
        onUpdated={() => {}}
        onUnauthorized={() => {}}
      />
    );
    const dialog = await screen.findByRole("dialog");

    await user.clear(within(dialog).getByLabelText("Nombre del evento"));
    await user.type(within(dialog).getByLabelText("Nombre del evento"), "Viaje a Mendoza");
    await user.click(within(dialog).getByRole("button", { name: /Guardar cambios/ }));

    await waitFor(() => expect(updateEvent).toHaveBeenCalledTimes(1));
    const [, payload] = vi.mocked(updateEvent).mock.calls[0];
    expect(payload).toEqual(expect.objectContaining({ name: "Viaje a Mendoza" }));
    expect(payload).not.toHaveProperty("description");
  });

  it("muestra un error claro y conserva los valores si falla el guardado", async () => {
    vi.mocked(updateEvent).mockRejectedValue(
      new EventError("forbidden", "Solo el dueño puede editar este evento.")
    );
    const user = userEvent.setup();
    renderDialog();
    const dialog = await screen.findByRole("dialog");

    await user.click(within(dialog).getByRole("button", { name: /Guardar cambios/ }));

    expect(
      await within(dialog).findByText("Solo el dueño puede editar este evento.")
    ).toBeInTheDocument();
    expect(within(dialog).getByLabelText("Nombre del evento")).toHaveValue("Viaje a Bariloche");
  });

  it("preserva iconKey como undefined al guardar si el evento no tiene icono", async () => {
    const eventWithoutIcon: EventDetail = {
      ...event,
      iconKey: null,
    };
    vi.mocked(updateEvent).mockResolvedValue({ ...eventWithoutIcon, name: "Viaje a Salta" });
    const user = userEvent.setup();
    render(
      <EditEventDialog
        open
        onOpenChange={() => {}}
        event={eventWithoutIcon}
        onUpdated={() => {}}
        onUnauthorized={() => {}}
      />
    );
    const dialog = await screen.findByRole("dialog");

    await user.clear(within(dialog).getByLabelText("Nombre del evento"));
    await user.type(within(dialog).getByLabelText("Nombre del evento"), "Viaje a Salta");

    await user.click(within(dialog).getByRole("button", { name: /Guardar cambios/ }));

    await waitFor(() =>
      expect(vi.mocked(updateEvent)).toHaveBeenCalledWith(
        1,
        expect.objectContaining({ name: "Viaje a Salta", iconKey: undefined })
      )
    );
  });
});
