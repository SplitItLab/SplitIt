import { describe, it, expect } from "vitest";
import { postLoginPath, registerSchema } from "@/lib/auth";

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

describe("postLoginPath", () => {
  it("vuelve a la ruta interna pedida", () => {
    expect(postLoginPath("/eventos/invitacion/abc")).toBe("/eventos/invitacion/abc");
  });

  it("usa mis eventos cuando no hay ruta pedida", () => {
    expect(postLoginPath(null)).toBe("/eventos");
  });

  it("ignora rutas a otros sitios", () => {
    expect(postLoginPath("https://otro-sitio.com")).toBe("/eventos");
    expect(postLoginPath("//otro-sitio.com")).toBe("/eventos");
  });
});
