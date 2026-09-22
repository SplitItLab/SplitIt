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

export type EventMember = {
  id: number;
  name: string;
  email?: string | null;
  isGuest: boolean;
};

export type EventDetail = {
  id: number;
  name: string;
  description?: string | null;
  iconKey?: string | null;
  baseCurrency: string;
  memberCount: number;
  members: EventMember[];
  isOwner: boolean;
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

/** Reutiliza las validaciones de creación: solo nombre, descripción e icono son editables. */
export const updateEventSchema = createEventSchema.pick({
  name: true,
  description: true,
  iconKey: true,
});

export type UpdateEventInput = z.input<typeof updateEventSchema>;
export type UpdateEventPayload = z.output<typeof updateEventSchema>;

export type EventErrorType =
  | "validation"
  | "unauthorized"
  | "forbidden"
  | "not-found"
  | "conflict"
  | "network"
  | "server-error";

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
    if (err.status === 401) {
      throw new EventError("unauthorized", "Tu sesión expiró. Iniciá sesión de nuevo.");
    }
    if (err.status === 403) {
      throw new EventError("forbidden", "No tenés acceso a este evento.");
    }
    if (err.status === 404) {
      throw new EventError("not-found", "No encontramos este evento.");
    }
    if (err.status === 400 || err.status === 422) {
      throw new EventError("validation", err.message || "Revisá los datos ingresados.");
    }
    if (err.status === 409) {
      throw new EventError(
        "conflict",
        err.message || "No se puede completar la operación porque tiene registros relacionados."
      );
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

export async function getEventById(id: number | string): Promise<EventDetail> {
  try {
    return await request<EventDetail>(`/api/events/${id}`);
  } catch (err) {
    toEventError(err);
  }
}

export async function updateEvent(
  id: number | string,
  input: UpdateEventPayload
): Promise<EventSummary> {
  try {
    return await request<EventSummary>(`/api/events/${id}`, {
      method: "PUT",
      body: JSON.stringify(input),
    });
  } catch (err) {
    if (err instanceof ApiError && err.status === 403) {
      throw new EventError("forbidden", "Solo el dueño puede editar este evento.");
    }
    toEventError(err);
  }
}

export async function deleteEvent(id: number | string): Promise<void> {
  try {
    await request<void>(`/api/events/${id}`, { method: "DELETE" });
  } catch (err) {
    if (err instanceof ApiError && err.status === 403) {
      throw new EventError("forbidden", "Solo el dueño puede eliminar este evento.");
    }
    if (err instanceof ApiError && err.status === 409) {
      throw new EventError(
        "conflict",
        "No se puede eliminar el evento porque tiene registros relacionados."
      );
    }
    toEventError(err);
  }
}

export type InviteLinkResponse = { token: string };

export async function getEventInviteToken(id: number | string): Promise<string> {
  try {
    const body = await request<InviteLinkResponse>(`/api/events/${id}/invite-link`, {
      method: "POST",
    });
    return body.token;
  } catch (err) {
    if (err instanceof ApiError && err.status === 403) {
      throw new EventError("forbidden", "Solo el dueño puede invitar a este evento.");
    }
    toEventError(err);
  }
}

export function buildEventInviteUrl(
  token: string,
  origin: string = globalThis.location.origin
): string {
  return `${origin}/eventos/invitacion/${token}`;
}

export function filterEventsByName(events: EventSummary[], query: string): EventSummary[] {
  const term = query.trim().toLowerCase();
  if (!term) return events;
  return events.filter((event) => event.name.toLowerCase().includes(term));
}

export function currencyLabel(code: string): string {
  return EVENT_CURRENCIES.find((c) => c.code === code)?.label ?? code;
}
