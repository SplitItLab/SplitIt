import { z } from "zod";
import { request, ApiError } from "@/lib/api";

export type EventSummary = {
  id: number;
  name: string;
  description?: string | null;
  iconKey?: string | null;
  baseCurrency: string;
  memberCount: number;
};

export const EVENT_CURRENCIES = [
  { code: "ARS", label: "ARS - Peso argentino" },
  { code: "USD", label: "USD - Dolar" },
  { code: "EUR", label: "EUR - Euro" },
  { code: "BRL", label: "BRL - Real" },
] as const;

export const DEFAULT_CURRENCY = "ARS";

const currencyCodes = EVENT_CURRENCIES.map((c) => c.code) as [string, ...string[]];

const optionalText = z
  .string()
  .trim()
  .transform((value) => (value === "" ? undefined : value))
  .optional();

export const createEventSchema = z.object({
  name: z.string().trim().min(1, "El nombre es obligatorio."),
  description: optionalText,
  iconKey: optionalText,
  baseCurrency: z.enum(currencyCodes, { message: "Elegí una moneda válida." }),
  participantNames: z
    .array(z.string().trim().min(1, "El nombre del integrante no puede estar vacío."))
    .default([]),
});

export type CreateEventInput = z.input<typeof createEventSchema>;
export type CreateEventPayload = z.output<typeof createEventSchema>;

export type EventErrorType = "validation" | "unauthorized" | "network" | "server-error";

export class EventError extends Error {
  constructor(
    public type: EventErrorType,
    message: string
  ) {
    super(message);
    this.name = "EventError";
  }
}

function toEventError(err: unknown): never {
  if (err instanceof ApiError) {
    if (err.status === 0) {
      throw new EventError("network", "No pudimos conectar con el servidor.");
    }
    if (err.status === 401 || err.status === 403) {
      throw new EventError("unauthorized", "Tu sesión expiró. Iniciá sesión de nuevo.");
    }
    if (err.status === 400 || err.status === 422) {
      throw new EventError("validation", err.message || "Revisá los datos ingresados.");
    }
    throw new EventError("server-error", "Ocurrió un error. Probá de nuevo.");
  }
  throw err as Error;
}

export async function listEvents(): Promise<EventSummary[]> {
  try {
    return await request<EventSummary[]>("/api/events");
  } catch (err) {
    toEventError(err);
  }
}

export async function createEvent(input: CreateEventPayload): Promise<EventSummary> {
  try {
    return await request<EventSummary>("/api/events", {
      method: "POST",
      body: JSON.stringify(input),
    });
  } catch (err) {
    toEventError(err);
  }
}

export function filterEventsByName(events: EventSummary[], query: string): EventSummary[] {
  const term = query.trim().toLowerCase();
  if (!term) return events;
  return events.filter((event) => event.name.toLowerCase().includes(term));
}

export function currencyLabel(code: string): string {
  return EVENT_CURRENCIES.find((c) => c.code === code)?.label ?? code;
}
