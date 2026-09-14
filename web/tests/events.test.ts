import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  createEvent,
  createEventSchema,
  EventError,
  filterEventsByName,
  listEvents,
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
