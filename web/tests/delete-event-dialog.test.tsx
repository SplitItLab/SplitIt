import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { DeleteEventDialog } from "../components/delete-event-dialog";
import { EventError, type EventDetail } from "../lib/events";

vi.mock("@/lib/events", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/events")>();
  return {
    ...actual,
    deleteEvent: vi.fn(),
  };
});

import { deleteEvent } from "@/lib/events";

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

function renderDialog(onDeleted = () => {}) {
  return render(
    <DeleteEventDialog
      open
      onOpenChange={() => {}}
      event={event}
      onDeleted={onDeleted}
      onUnauthorized={() => {}}
    />
  );
}

describe("DeleteEventDialog", () => {
  beforeEach(() => {
    vi.mocked(deleteEvent).mockReset();
  });

  it("muestra el nombre del evento y la advertencia de la acción", async () => {
    renderDialog();
    const dialog = await screen.findByRole("dialog");

    expect(within(dialog).getByText("Eliminar «Viaje a Bariloche»")).toBeInTheDocument();
    expect(within(dialog).getByText(/Esta acción no se puede deshacer/)).toBeInTheDocument();
  });

  it("cancelar cierra sin enviar ninguna request", async () => {
    const onOpenChange = vi.fn();
    const user = userEvent.setup();
    render(
      <DeleteEventDialog
        open
        onOpenChange={onOpenChange}
        event={event}
        onDeleted={() => {}}
        onUnauthorized={() => {}}
      />
    );
    const dialog = await screen.findByRole("dialog");

    await user.click(within(dialog).getByRole("button", { name: "Cancelar" }));

    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(deleteEvent).not.toHaveBeenCalled();
  });

  it("confirmar envía una sola request aunque se haga doble click", async () => {
    let resolveDelete!: (value: void) => void;
    vi.mocked(deleteEvent).mockReturnValue(
      new Promise<void>((resolve) => {
        resolveDelete = resolve;
      })
    );
    const onDeleted = vi.fn();
    const user = userEvent.setup();
    renderDialog(onDeleted);
    const dialog = await screen.findByRole("dialog");

    const confirm = within(dialog).getByRole("button", { name: "Eliminar evento" });
    await user.click(confirm);
    await waitFor(() => expect(confirm).toBeDisabled());
    await user.click(confirm);

    expect(deleteEvent).toHaveBeenCalledTimes(1);
    expect(deleteEvent).toHaveBeenCalledWith(10);

    resolveDelete();
    await waitFor(() => expect(onDeleted).toHaveBeenCalledTimes(1));
  });

  it("muestra un error claro si falla la eliminación", async () => {
    vi.mocked(deleteEvent).mockRejectedValue(
      new EventError("server-error", "Ocurrió un error. Probá de nuevo.")
    );
    const user = userEvent.setup();
    renderDialog();
    const dialog = await screen.findByRole("dialog");

    await user.click(within(dialog).getByRole("button", { name: "Eliminar evento" }));

    expect(
      await within(dialog).findByText("Ocurrió un error. Probá de nuevo.")
    ).toBeInTheDocument();
    expect(deleteEvent).toHaveBeenCalledTimes(1);
  });

  it("muestra mensaje de conflicto cuando el evento tiene registros relacionados", async () => {
    vi.mocked(deleteEvent).mockRejectedValue(
      new EventError(
        "conflict",
        "No se puede eliminar el evento porque tiene registros relacionados."
      )
    );
    const user = userEvent.setup();
    renderDialog();
    const dialog = await screen.findByRole("dialog");

    await user.click(within(dialog).getByRole("button", { name: "Eliminar evento" }));

    expect(
      await within(dialog).findByText(
        "No se puede eliminar el evento porque tiene registros relacionados."
      )
    ).toBeInTheDocument();
  });
});
