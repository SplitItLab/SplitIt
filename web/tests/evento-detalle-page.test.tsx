import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor, cleanup } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

vi.mock("@/lib/events", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/events")>();
  return { ...actual, getEvent: vi.fn() };
});

import { EventError, getEvent, type EventDetail } from "@/lib/events";
import { EventDetailView } from "../components/event-detail-view";

const eventDetail: EventDetail = {
  id: 1,
  name: "Viaje a Bariloche",
  description: "Vacaciones",
  iconKey: "plane",
  baseCurrency: "ARS",
  memberCount: 2,
  members: [
    { id: 10, name: "Ana", email: "ana@mail.com", isGuest: false },
    { id: 11, name: "Juan", email: null, isGuest: true },
  ],
};

describe("EventDetailView", () => {
  beforeEach(() => {
    replace.mockClear();
    vi.mocked(getEvent).mockReset();
  });

  afterEach(() => {
    cleanup();
  });

  it("muestra la pantalla sin acceso cuando la API responde 403", async () => {
    vi.mocked(getEvent).mockRejectedValue(new EventError("forbidden", "No tenés acceso."));

    render(<EventDetailView id="1" />);

    expect(
      await screen.findByRole("heading", { name: "No encontramos este evento" })
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Ir a mis eventos" })).toHaveAttribute(
      "href",
      "/eventos"
    );
  });

  it("no filtra datos del evento ni redirige al login cuando recibe un 403", async () => {
    vi.mocked(getEvent).mockRejectedValue(new EventError("forbidden", "No tenés acceso."));

    render(<EventDetailView id="1" />);

    await screen.findByRole("heading", { name: "No encontramos este evento" });
    expect(screen.queryByText("Viaje a Bariloche")).not.toBeInTheDocument();
    expect(replace).not.toHaveBeenCalled();
  });

  it("muestra la misma pantalla cuando el evento no existe", async () => {
    vi.mocked(getEvent).mockRejectedValue(new EventError("not-found", "No encontramos el evento."));

    render(<EventDetailView id="999" />);

    expect(
      await screen.findByRole("heading", { name: "No encontramos este evento" })
    ).toBeInTheDocument();
  });

  it("permite reintentar después de un error de red", async () => {
    vi.mocked(getEvent)
      .mockRejectedValueOnce(new EventError("network", "No pudimos conectar con el servidor."))
      .mockResolvedValueOnce(eventDetail);

    render(<EventDetailView id="1" />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No pudimos conectar con el servidor."
    );

    await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));

    expect(await screen.findByRole("heading", { name: "Viaje a Bariloche" })).toBeInTheDocument();
    expect(getEvent).toHaveBeenCalledTimes(2);
  });

  it("redirige al login cuando la sesión no es válida", async () => {
    vi.mocked(getEvent).mockRejectedValue(new EventError("unauthorized", "Tu sesión expiró."));

    render(<EventDetailView id="1" />);

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
  });
  it("muestra nombre, descripción y moneda del evento", async () => {
    vi.mocked(getEvent).mockResolvedValue(eventDetail);

    render(<EventDetailView id="1" />);

    expect(await screen.findByRole("heading", { name: "Viaje a Bariloche" })).toBeInTheDocument();
    expect(screen.getByText("Vacaciones")).toBeInTheDocument();
    expect(screen.getByText("Moneda ARS - Peso argentino")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Volver a eventos" })).toHaveAttribute(
      "href",
      "/eventos"
    );
  });

  it("permite navegar entre Gastos, Saldos e Integrantes", async () => {
    vi.mocked(getEvent).mockResolvedValue(eventDetail);

    render(<EventDetailView id="1" />);

    expect(await screen.findByRole("tab", { name: "Gastos" })).toHaveAttribute(
      "aria-selected",
      "true"
    );
    expect(screen.getByText("Todavía no hay gastos")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("tab", { name: "Saldos" }));
    expect(screen.getByText("Todavía no hay saldos")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("tab", { name: "Integrantes" }));
    expect(screen.getByRole("heading", { name: "Integrantes" })).toBeInTheDocument();
  });

  it("muestra el email de los integrantes con cuenta y marca invitado a los que no", async () => {
    vi.mocked(getEvent).mockResolvedValue(eventDetail);

    render(<EventDetailView id="1" />);

    await userEvent.click(await screen.findByRole("tab", { name: "Integrantes" }));

    expect(screen.getByText("Ana")).toBeInTheDocument();
    expect(screen.getByText("ana@mail.com")).toBeInTheDocument();

    expect(screen.getByText("Juan")).toBeInTheDocument();
    expect(screen.getByText("Invitado")).toBeInTheDocument();
  });
});
