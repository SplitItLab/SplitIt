import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import EventsPage from "../app/(private)/eventos/page";
import { EventError, type EventSummary } from "../lib/events";

const replace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

vi.mock("@/lib/use-session", () => ({
  useSession: () => ({
    status: "authenticated",
    user: { id: 1, name: "Nicolas", email: "nico@example.com" },
  }),
}));

vi.mock("@/lib/events", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/events")>();
  return {
    ...actual,
    listEvents: vi.fn(),
    createEvent: vi.fn(),
  };
});

import { listEvents, createEvent } from "@/lib/events";

const events: EventSummary[] = [
  {
    id: 1,
    name: "Viaje a Bariloche",
    description: "Vacaciones de verano con amigos",
    iconKey: "plane",
    baseCurrency: "ARS",
    memberCount: 4,
  },
  {
    id: 2,
    name: "Depto compartido",
    description: "Gastos mensuales del departamento",
    iconKey: "house",
    baseCurrency: "USD",
    memberCount: 3,
  },
];

describe("EventsPage", () => {
  beforeEach(() => {
    replace.mockReset();
    vi.mocked(listEvents).mockReset();
    vi.mocked(createEvent).mockReset();
  });

  it("pide los eventos al abrir la pantalla y muestra los campos del contrato", async () => {
    vi.mocked(listEvents).mockResolvedValue(events);

    render(<EventsPage />);

    expect(listEvents).toHaveBeenCalledTimes(1);
    expect(await screen.findByText("Viaje a Bariloche")).toBeInTheDocument();
    expect(screen.getByText("Vacaciones de verano con amigos")).toBeInTheDocument();
    expect(screen.getByText("4 integrantes · ARS")).toBeInTheDocument();
    expect(screen.getByText("3 integrantes · USD")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Viaje a Bariloche/ })).toHaveAttribute(
      "href",
      "/eventos/1"
    );
  });

  it("muestra el estado vacío cuando la API devuelve una lista vacía", async () => {
    vi.mocked(listEvents).mockResolvedValue([]);

    render(<EventsPage />);

    expect(await screen.findByText("Todavía no tenés eventos")).toBeInTheDocument();
    expect(screen.queryByLabelText("Buscar evento")).not.toBeInTheDocument();
  });

  it("filtra por nombre sin distinguir mayúsculas y restaura al limpiar", async () => {
    vi.mocked(listEvents).mockResolvedValue(events);
    const user = userEvent.setup();

    render(<EventsPage />);
    await screen.findByText("Viaje a Bariloche");

    const searchInput = screen.getByLabelText("Buscar evento");
    expect(searchInput).toHaveAttribute("type", "text");

    await user.type(searchInput, "bariLO");
    expect(screen.getByText("Viaje a Bariloche")).toBeInTheDocument();
    expect(screen.queryByText("Depto compartido")).not.toBeInTheDocument();

    await user.click(screen.getByLabelText("Limpiar búsqueda"));
    expect(searchInput).toHaveValue("");
    expect(screen.getByText("Depto compartido")).toBeInTheDocument();
  });

  it("distingue una búsqueda sin coincidencias del estado vacío", async () => {
    vi.mocked(listEvents).mockResolvedValue(events);
    const user = userEvent.setup();

    render(<EventsPage />);
    await screen.findByText("Viaje a Bariloche");

    await user.type(screen.getByLabelText("Buscar evento"), "zzz");

    expect(screen.getByText("Sin resultados")).toBeInTheDocument();
    expect(screen.queryByText("Todavía no tenés eventos")).not.toBeInTheDocument();
  });

  it("muestra un error de carga y permite reintentar", async () => {
    vi.mocked(listEvents)
      .mockRejectedValueOnce(new EventError("network", "No pudimos conectar con el servidor."))
      .mockResolvedValueOnce(events);
    const user = userEvent.setup();

    render(<EventsPage />);

    expect(await screen.findByText("No pudimos conectar con el servidor.")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Reintentar" }));

    expect(await screen.findByText("Viaje a Bariloche")).toBeInTheDocument();
  });

  it("redirige al login cuando la sesión expiró", async () => {
    vi.mocked(listEvents).mockRejectedValue(new EventError("unauthorized", "Tu sesión expiró."));

    render(<EventsPage />);

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
  });

  it("no envía la request cuando el nombre es inválido", async () => {
    vi.mocked(listEvents).mockResolvedValue(events);
    const user = userEvent.setup();

    render(<EventsPage />);
    await screen.findByText("Viaje a Bariloche");

    await user.click(screen.getByRole("button", { name: /Crear evento/ }));
    const dialog = await screen.findByRole("dialog");

    await user.click(within(dialog).getByRole("button", { name: /Crear evento/ }));

    await waitFor(() => expect(screen.getByText("El nombre es obligatorio.")).toBeInTheDocument());
    expect(createEvent).not.toHaveBeenCalled();
  });

  it("crea un evento con una sola request y lo agrega a la lista", async () => {
    vi.mocked(listEvents).mockResolvedValue(events);
    vi.mocked(createEvent).mockResolvedValue({
      id: 3,
      name: "Viaje a Mendoza",
      description: "Gastos del fin de semana",
      iconKey: "plane",
      baseCurrency: "ARS",
      memberCount: 2,
    });
    const user = userEvent.setup();

    render(<EventsPage />);
    await screen.findByText("Viaje a Bariloche");

    await user.click(screen.getByRole("button", { name: /Crear evento/ }));
    const dialog = await screen.findByRole("dialog");

    await user.type(within(dialog).getByLabelText("Nombre del evento"), "Viaje a Mendoza");
    await user.type(within(dialog).getByLabelText(/Descripción/), "Gastos del fin de semana");
    await user.type(within(dialog).getByLabelText("Integrantes"), "  Ana  ");
    await user.click(within(dialog).getByRole("button", { name: "Agregar" }));

    const submit = within(dialog).getByRole("button", { name: /Crear evento/ });
    await user.click(submit);
    await user.click(submit);

    await waitFor(() => expect(createEvent).toHaveBeenCalledTimes(1));
    expect(vi.mocked(createEvent).mock.calls[0][0]).toMatchObject({
      name: "Viaje a Mendoza",
      description: "Gastos del fin de semana",
      baseCurrency: "ARS",
      participantNames: ["Ana"],
    });
    expect(await screen.findByText("Viaje a Mendoza")).toBeInTheDocument();
  });

  it("preserva un evento recién creado cuando termina la carga inicial", async () => {
    let resolveInitialLoad!: (value: EventSummary[]) => void;
    vi.mocked(listEvents).mockImplementationOnce(
      () => new Promise<EventSummary[]>((resolve) => (resolveInitialLoad = resolve))
    );
    vi.mocked(createEvent).mockResolvedValue({
      id: 3,
      name: "Viaje a Mendoza",
      description: "Gastos del fin de semana",
      iconKey: "plane",
      baseCurrency: "ARS",
      memberCount: 2,
    });
    const user = userEvent.setup();

    render(<EventsPage />);

    await user.click(screen.getByRole("button", { name: /Crear evento/ }));
    const dialog = await screen.findByRole("dialog");
    await user.type(within(dialog).getByLabelText("Nombre del evento"), "Viaje a Mendoza");
    await user.click(within(dialog).getByRole("button", { name: /Crear evento/ }));

    expect(await screen.findByText("Viaje a Mendoza")).toBeInTheDocument();

    resolveInitialLoad(events);

    await waitFor(() => {
      expect(screen.getByText("Viaje a Mendoza")).toBeInTheDocument();
      expect(screen.queryByText("Viaje a Bariloche")).not.toBeInTheDocument();
    });
  });

  it("muestra un mensaje claro si falla la creación", async () => {
    vi.mocked(listEvents).mockResolvedValue(events);
    vi.mocked(createEvent).mockRejectedValue(
      new EventError("network", "No pudimos conectar con el servidor.")
    );
    const user = userEvent.setup();

    render(<EventsPage />);
    await screen.findByText("Viaje a Bariloche");

    await user.click(screen.getByRole("button", { name: /Crear evento/ }));
    const dialog = await screen.findByRole("dialog");

    await user.type(within(dialog).getByLabelText("Nombre del evento"), "Asado");
    await user.click(within(dialog).getByRole("button", { name: /Crear evento/ }));

    expect(
      await within(dialog).findByText("No pudimos conectar con el servidor.")
    ).toBeInTheDocument();
  });
});
