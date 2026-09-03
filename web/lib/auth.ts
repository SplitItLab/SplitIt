import { request, ApiError } from "@/lib/api";
import { z } from "zod";

const USE_MOCK = process.env.NEXT_PUBLIC_MOCK_AUTH === "true";

const MOCK_SESSION_KEY = "mock_session_user";

export const registerSchema = z.object({
  name: z.string().min(1, "El nombre es obligatorio."),
  email: z.email("Ingresá un email válido."),
  password: z.string().min(8, "Debe tener al menos 8 caracteres."),
});

export type RegisterInput = z.infer<typeof registerSchema>;

export type RegisterErrorType = "invalid-data" | "email-taken" | "network" | "server-error";

export class RegisterError extends Error {
  constructor(
    public type: RegisterErrorType,
    message: string
  ) {
    super(message);
    this.name = "RegisterError";
  }
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

export type LogoutErrorType = "network" | "server-error";

export class LogoutError extends Error {
  constructor(
    public type: LogoutErrorType,
    message: string
  ) {
    super(message);
    this.name = "LogoutError";
  }
}

async function mockRegister(input: RegisterInput): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 600));

  if (input.email === "taken@example.com") {
    throw new RegisterError("email-taken", "Ese email ya está registrado.");
  }
  if (input.email === "network@example.com") {
    throw new RegisterError("network", "No pudimos conectar con el servidor.");
  }
  if (input.email === "error@example.com") {
    throw new RegisterError("server-error", "Probá de nuevo en un momento.");
  }
}

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

async function mockLogout(): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 300));
  sessionStorage.removeItem(MOCK_SESSION_KEY);
}

export async function registerUser(input: RegisterInput): Promise<void> {
  if (USE_MOCK) {
    return mockRegister(input);
  }
  try {
    await request<void>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(input),
    });
  } catch (err) {
    if (err instanceof ApiError) {
      if (err.status === 0) {
        throw new RegisterError("network", "No pudimos conectar con el servidor.");
      }
      if (err.status === 400) {
        throw new RegisterError("invalid-data", "Revisá los datos ingresados.");
      }
      if (err.status === 409) {
        throw new RegisterError("email-taken", "Ese email ya está registrado.");
      }
      throw new RegisterError(
        "server-error",
        "Algo salió mal de nuestro lado. Probá de nuevo en un momento."
      );
    }
    throw err;
  }
}

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

export async function logout(): Promise<void> {
  if (USE_MOCK) {
    return mockLogout();
  }
  try {
    await request<void>("/api/auth/logout", { method: "POST" });
  } catch (err) {
    if (err instanceof ApiError) {
      if (err.status === 0) {
        throw new LogoutError("network", "No pudimos conectar con el servidor.");
      }
      throw new LogoutError("server-error", "No pudimos cerrar la sesión. Probá de nuevo.");
    }
    throw err;
  }
}
