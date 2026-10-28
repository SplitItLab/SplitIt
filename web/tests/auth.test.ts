import { describe, it, expect } from "vitest";
import { postLoginPath, registerSchema, withRedirect } from "@/lib/auth";

const unsafeRedirects = [
  "/\t/otro-sitio.com",
  "/\n/otro-sitio.com",
  "/\r/otro-sitio.com",
  "/\t\\otro-sitio.com",
  "/eventos\\invitacion/abc",
  "/eventos?next=\\otro-sitio.com",
  ...Array.from({ length: 32 }, (_, code) => `/eventos${String.fromCharCode(code)}`),
  ...Array.from({ length: 33 }, (_, code) => `/eventos${String.fromCharCode(127 + code)}`),
];

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
  it.each(unsafeRedirects)("descarta la ruta insegura %j", (redirect) => {
    expect(postLoginPath(redirect)).toBe("/eventos");
  });

  it("vuelve a la ruta interna pedida", () => {
    expect(postLoginPath("/eventos/invitacion/abc")).toBe("/eventos/invitacion/abc");
  });

  it("usa mis eventos cuando no hay ruta pedida", () => {
    expect(postLoginPath(null)).toBe("/eventos");
  });

  it("ignora rutas a otros sitios", () => {
    expect(postLoginPath("https://otro-sitio.com")).toBe("/eventos");
    expect(postLoginPath("//otro-sitio.com")).toBe("/eventos");
    expect(postLoginPath("/\\otro-sitio.com")).toBe("/eventos");
  });

  it("ignora rutas que no empiezan con barra", () => {
    expect(postLoginPath("eventos")).toBe("/eventos");
  });
});

describe("withRedirect", () => {
  it.each(unsafeRedirects)("descarta la ruta de vuelta insegura %j", (redirect) => {
    expect(withRedirect("/login", redirect)).toBe("/login");
  });

  it("agrega la ruta de vuelta cuando es interna", () => {
    expect(withRedirect("/register", "/eventos/invitacion/abc")).toBe(
      `/register?redirect=${encodeURIComponent("/eventos/invitacion/abc")}`
    );
  });

  it("deja la ruta como está cuando no hay ruta de vuelta", () => {
    expect(withRedirect("/login", null)).toBe("/login");
  });

  it("descarta rutas de vuelta a otros sitios", () => {
    expect(withRedirect("/login", "https://otro-sitio.com")).toBe("/login");
    expect(withRedirect("/login", "//otro-sitio.com")).toBe("/login");
    expect(withRedirect("/login", "/\\otro-sitio.com")).toBe("/login");
  });

  it("ignora redirecciones externas con barras invertidas", () => {
    expect(postLoginPath("/\\otro-sitio.com")).toBe("/eventos");
  });

  it("ignora redirecciones externas con tabulaciones", () => {
    expect(postLoginPath("/\t/otro-sitio.com")).toBe("/eventos");
  });

  it("ignora URLs inválidas", () => {
    expect(postLoginPath("/\\[invalid")).toBe("/eventos");
  });

  it("ignora rutas que se normalizan a una referencia a otro sitio", () => {
    expect(postLoginPath("/eventos/..//otro-sitio.com")).toBe("/eventos");
  });

  it("normaliza la ruta interna y conserva los parámetros y el fragmento", () => {
    expect(postLoginPath("/eventos/../eventos/invitacion/abc?origen=login#miembro")).toBe(
      "/eventos/invitacion/abc?origen=login#miembro"
    );
  });
});
