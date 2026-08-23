import { request, ApiError } from "@/lib/api";
import { z } from "zod";

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

export async function registerUser(input: RegisterInput): Promise<void> {
  try {
    await request<void>("/auth/register", {
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
