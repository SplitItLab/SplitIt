import { describe, it, expect } from "vitest";
import { registerSchema } from "@/lib/auth";

describe("registerSchema", () => {
  it("acepta datos válidos", () => {
    const result = registerSchema.safeParse({
      name: "Ada Lovelace",
      email: "ada@example.com",
      password: "una-clave-segura",
    });
    expect(result.success).toBe(true);
  });

  it("rechaza el nombre vacío", () => {
    const result = registerSchema.safeParse({
      name: "",
      email: "ada@example.com",
      password: "una-clave-segura",
    });
    expect(result.success).toBe(false);
  });

  it("rechaza un email con formato inválido", () => {
    const result = registerSchema.safeParse({
      name: "Ada Lovelace",
      email: "no-es-un-email",
      password: "una-clave-segura",
    });
    expect(result.success).toBe(false);
  });

  it("rechaza una contraseña de menos de 8 caracteres", () => {
    const result = registerSchema.safeParse({
      name: "Ada Lovelace",
      email: "ada@example.com",
      password: "1234567",
    });
    expect(result.success).toBe(false);
  });
});
