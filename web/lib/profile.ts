import { z } from "zod";
import { request, ApiError } from "@/lib/api";
import { SessionUser } from "@/lib/auth";

const USE_MOCK = process.env.NEXT_PUBLIC_MOCK_AUTH === "true";
const MOCK_PROFILE_KEY = "mock_profile_user";

export type Profile = SessionUser;

function readMockProfile(): Profile {
  const raw = sessionStorage.getItem(MOCK_PROFILE_KEY);
  if (raw) return JSON.parse(raw) as Profile;
  return { id: 1, name: "Ada Lovelace", email: "ada@example.com" };
}

async function mockGetProfile(): Promise<Profile> {
  await new Promise((resolve) => setTimeout(resolve, 300));
  return readMockProfile();
}

async function mockUpdateProfile(input: ProfileInput): Promise<Profile> {
  await new Promise((resolve) => setTimeout(resolve, 600));
  if (input.email === "taken@example.com") {
    throw new ProfileError("email-taken", "Ese email ya está registrado.", "email");
  }
  const updated = { ...readMockProfile(), ...input };
  sessionStorage.setItem(MOCK_PROFILE_KEY, JSON.stringify(updated));
  return updated;
}

export const profileSchema = z.object({
  name: z.string().trim().min(1, "El nombre es obligatorio."),
  email: z.string().trim().min(1, "El email es obligatorio.").email("El email no es válido."),
});

export type ProfileInput = z.infer<typeof profileSchema>;

export type ProfileErrorType =
  "validation" | "email-taken" | "unauthorized" | "network" | "server-error";

export class ProfileError extends Error {
  constructor(
    public type: ProfileErrorType,
    message: string,
    public field?: "name" | "email"
  ) {
    super(message);
    this.name = "ProfileError";
  }
}

function toKnownField(field?: string): "name" | "email" | undefined {
  if (field === "name" || field === "email") return field;
  return undefined;
}

function guessFieldFromMessage(message: string): "name" | "email" | undefined {
  if (/e-?mail|correo/i.test(message)) return "email";
  if (/nombre|name/i.test(message)) return "name";
  return undefined;
}

function toProfileError(err: unknown): never {
  if (err instanceof ApiError) {
    if (err.status === 401) {
      throw new ProfileError("unauthorized", "Tu sesión expiró. Iniciá sesión de nuevo.");
    }
    if (err.status === 409) {
      throw new ProfileError("email-taken", "Ese email ya está registrado.", "email");
    }
    if (err.status === 400) {
      const message = err.message || "Revisá los datos ingresados.";
      const field = toKnownField(err.field) ?? guessFieldFromMessage(message);
      throw new ProfileError("validation", message, field);
    }
    if (err.status === 0) {
      throw new ProfileError("network", "No pudimos conectar con el servidor.");
    }
    throw new ProfileError("server-error", "Ocurrió un error. Probá de nuevo.");
  }
  throw err as Error;
}

export async function getProfile(): Promise<Profile> {
  if (USE_MOCK) {
    return mockGetProfile();
  }
  try {
    return await request<Profile>("/api/me");
  } catch (err) {
    toProfileError(err);
  }
}

export async function updateProfile(input: ProfileInput): Promise<Profile> {
  if (USE_MOCK) {
    return mockUpdateProfile(input);
  }
  try {
    return await request<Profile>("/api/me", {
      method: "PATCH",
      body: JSON.stringify(input),
    });
  } catch (err) {
    toProfileError(err);
  }
}

export function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "";
  const first = parts[0][0];
  const last = parts.length > 1 ? parts[parts.length - 1][0] : "";
  return `${first}${last}`.toUpperCase();
}
