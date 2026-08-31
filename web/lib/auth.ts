import z from "zod";
import { request, ApiError } from "@/lib/api";

const USE_MOCK = process.env.NEXT_PUBLIC_MOCK_AUTH === "true";

const MOCK_SESSION_KEY = "mock_session_user";

async function mockLogin(input: LoginInput): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 600));

  if (input.email !== "ada@example.com" || input.password !== "una-clave-segura") {
    throw new LoginError("invalid-credentials", "Email o contraseña incorrectos.");
  }

  const user: SessionUser = { id: 1, name: "Ada Lovelace", email: input.email };
  sessionStorage.setItem(MOCK_SESSION_KEY, JSON.stringify(user));
}

async function mockGetSession(): Promise<SessionUser | null> {
  await new Promise((resolve) => setTimeout(resolve, 300));
  const raw = sessionStorage.getItem(MOCK_SESSION_KEY);
  return raw ? (JSON.parse(raw) as SessionUser) : null;
}

export const loginSchema = z.object({
  email: z.string().min(1, "El email es obligatorio."),
  password: z.string().min(1, "La contraseña es obligatoria."),
});

export type LoginInput = z.infer<typeof loginSchema>;

export type LoginErrorType = "invalid-credentials" | "network" | "server-error";

export class LoginError extends Error {
  constructor(
    public type: LoginErrorType,
    message: string
  ) {
    super(message);
    this.name = "LoginError";
  }
}

export type SessionUser = {
  id: number;
  name: string;
  email: string;
};

export async function login(input: LoginInput): Promise<void> {
  if (USE_MOCK) {
    return mockLogin(input);
  }
  try {
    await request<void>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(input),
    });
  } catch (err) {
    if (err instanceof ApiError) {
      if (err.status === 401) {
        throw new LoginError("invalid-credentials", "Email o contraseña incorrectos.");
      }
      if (err.status === 0) {
        throw new LoginError("network", "No pudimos conectar con el servidor.");
      }
      throw new LoginError("server-error", "Probá de nuevo en un momento.");
    }
    throw err;
  }
}

export async function getSession(): Promise<SessionUser | null> {
  if (USE_MOCK) {
    return mockGetSession();
  }
  try {
    const user = await request<SessionUser | null>("/api/auth/session");
    return user ?? null;
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) {
      return null;
    }
    throw err;
  }
}
