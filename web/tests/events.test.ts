import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  createEvent,
  createEventSchema,
  deleteEvent,
  EventError,
  filterEventsByName,
  getEventById,
  listEvents,
  updateEvent,
  updateEventSchema,
  type EventDetail,
  type EventSummary,
} from "../lib/events";

const events: EventSummary[] = [
  {
    id: 1,
    name: "Viaje a Bariloche",
    description: "Vacaciones",
    iconKey: "plane",
    baseCurrency: "ARS",
    memberCount: 4,
  },
  {
    id: 2,
    name: "Depto compartido",
    description: null,
    iconKey: "house",
    baseCurrency: "ARS",
    memberCount: 3,
  },
];

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("filterEventsByName", () => {
  it("devuelve todos los eventos cuando la búsqueda está vacía", () => {
    expect(filterEventsByName(events, "   ")).toHaveLength(2);
  });

  it("filtra por nombre sin distinguir mayúsculas", () => {
    expect(filterEventsByName(events, "bariLOche")).toEqual([events[0]]);
  });

  it("devuelve una lista vacía cuando no hay coincidencias", () => {
    expect(filterEventsByName(events, "zzz")).toEqual([]);
  });
});

describe("createEventSchema", () => {
  it("rechaza un nombre vacío", () => {
    const result = createEventSchema.safeParse({
      name: "   ",
      baseCurrency: "ARS",
      participantNames: [],
    });
    expect(result.success).toBe(false);
  });

  it("rechaza una moneda inválida", () => {
    const result = createEventSchema.safeParse({
      name: "Asado",
      baseCurrency: "XXX",
      participantNames: [],
    });
    expect(result.success).toBe(false);
  });

  it("rechaza participantes vacíos", () => {
    const result = createEventSchema.safeParse({
      name: "Asado",
      baseCurrency: "ARS",
      participantNames: ["Ana", "  "],
    });
    expect(result.success).toBe(false);
  });

  it("recorta espacios y omite los campos opcionales vacíos", () => {
    const result = createEventSchema.parse({
      name: "  Asado  ",
      description: "   ",
      iconKey: "",
      baseCurrency: "ARS",
      participantNames: ["  Ana  ", "Juan"],
    });
    expect(result).toEqual({
      name: "Asado",
      description: undefined,
      iconKey: undefined,
      baseCurrency: "ARS",
      participantNames: ["Ana", "Juan"],
    });
  });
});

describe("listEvents y createEvent", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("pide GET /api/events y devuelve la lista", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(events));

    await expect(listEvents()).resolves.toEqual(events);

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(String(url)).toContain("/api/events");
    expect(init?.method ?? "GET").toBe("GET");
  });

  it("devuelve una lista vacía cuando la cuenta no tiene eventos", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse([]));
    await expect(listEvents()).resolves.toEqual([]);
  });

  it("traduce un fallo de red a un EventError de tipo network", async () => {
    vi.mocked(fetch).mockRejectedValue(new TypeError("failed"));

    await expect(listEvents()).rejects.toMatchObject({
      name: "EventError",
      type: "network",
    });
  });

  it("envía POST /api/events con el contrato acordado", async () => {
    const created = { ...events[0], id: 9, memberCount: 3 };
    vi.mocked(fetch).mockResolvedValue(jsonResponse(created, 201));

    const result = await createEvent({
      name: "Viaje a Mendoza",
      description: "Gastos del fin de semana",
      iconKey: "car",
      baseCurrency: "ARS",
      participantNames: ["Ana", "Juan"],
    });

    expect(result).toEqual(created);
    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(String(url)).toContain("/api/events");
    expect(init?.method).toBe("POST");
    expect(JSON.parse(String(init?.body))).toEqual({
      name: "Viaje a Mendoza",
      description: "Gastos del fin de semana",
      iconKey: "car",
      baseCurrency: "ARS",
      participantNames: ["Ana", "Juan"],
    });
  });

  it("traduce un 401 a un EventError de tipo unauthorized", async () => {
    vi.mocked(fetch).mockImplementation(async () => jsonResponse({ message: "no" }, 401));

    await expect(listEvents()).rejects.toBeInstanceOf(EventError);
    await expect(listEvents()).rejects.toMatchObject({ type: "unauthorized" });
  });
});

describe("getEventById y updateEvent", () => {
  const detail: EventDetail = {
    id: 1,
    name: "Viaje a Bariloche",
    description: "Vacaciones",
    iconKey: "plane",
    baseCurrency: "ARS",
    memberCount: 4,
    members: [],
  };

  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("pide GET /api/events/{id} y devuelve el detalle", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse(detail));

    await expect(getEventById(1)).resolves.toEqual(detail);

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(String(url)).toContain("/api/events/1");
    expect(init?.method ?? "GET").toBe("GET");
  });

  it("traduce un 403 del detalle a un EventError de tipo forbidden", async () => {
    vi.mocked(fetch).mockImplementation(async () => jsonResponse({ message: "Forbidden" }, 403));

    await expect(getEventById(1)).rejects.toMatchObject({ type: "forbidden" });
  });

  it("el schema de edición reutiliza la validación de nombre y no incluye moneda", async () => {
    expect(updateEventSchema.safeParse({ name: "   ", description: "x" }).success).toBe(false);
    expect("baseCurrency" in updateEventSchema.shape).toBe(false);
    expect("participantNames" in updateEventSchema.shape).toBe(false);
  });

  it("envía PUT /api/events/{id} solo con nombre, descripción e icono", async () => {
    const updated = { ...events[0], name: "Viaje a Mendoza" };
    vi.mocked(fetch).mockResolvedValue(jsonResponse(updated));

    const result = await updateEvent(1, {
      name: "Viaje a Mendoza",
      description: "Fin de semana",
      iconKey: "car",
    });

    expect(result).toEqual(updated);
    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(String(url)).toContain("/api/events/1");
    expect(init?.method).toBe("PUT");
    expect(JSON.parse(String(init?.body))).toEqual({
      name: "Viaje a Mendoza",
      description: "Fin de semana",
      iconKey: "car",
    });
  });

  it("traduce un 403 al editar a un EventError de tipo forbidden", async () => {
    vi.mocked(fetch).mockImplementation(async () => jsonResponse({ message: "Forbidden" }, 403));

    await expect(updateEvent(1, { name: "x" })).rejects.toMatchObject({ type: "forbidden" });
  });
});

describe("deleteEvent", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("envía DELETE /api/events/{id} y resuelve con 204", async () => {
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 204 }));

    await expect(deleteEvent(10)).resolves.toBeUndefined();

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(String(url)).toContain("/api/events/10");
    expect(init?.method).toBe("DELETE");
  });

  it("traduce un 403 a un EventError de tipo forbidden", async () => {
    vi.mocked(fetch).mockImplementation(async () => jsonResponse({ message: "Forbidden" }, 403));

    await expect(deleteEvent(10)).rejects.toMatchObject({ type: "forbidden" });
  });

  it("traduce un 409 a un EventError de tipo conflict con mensaje descriptivo", async () => {
    vi.mocked(fetch).mockImplementation(async () =>
      jsonResponse({ message: "Event has related records" }, 409)
    );

    await expect(deleteEvent(10)).rejects.toMatchObject({
      type: "conflict",
      message: "No se puede eliminar el evento porque tiene registros relacionados.",
    });
  });
});
