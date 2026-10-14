import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock("next/image", () => ({
  default: ({ src, className }: { src: string; className?: string }) => (
    // Test double for next/image.
    // eslint-disable-next-line @next/next/no-img-element
    <img src={src} className={className} alt="" />
  ),
}));

import LoginPage from "../app/login/page";
import RegisterPage from "../app/register/page";

describe("pantallas de autenticación", () => {
  it("login invita a registrarse y el logo vuelve al inicio", () => {
    render(<LoginPage />);

    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Iniciá sesión");
    expect(screen.getByRole("link", { name: "Registrate gratis" })).toHaveAttribute(
      "href",
      "/register"
    );
    expect(screen.getByRole("link", { name: "SplitIt" })).toHaveAttribute("href", "/");
  });

  it("registro invita a iniciar sesión con un único link", () => {
    render(<RegisterPage />);

    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Crea una cuenta");
    expect(screen.getByRole("link", { name: "Iniciá sesión" })).toHaveAttribute("href", "/login");
    expect(screen.queryByRole("link", { name: "LogIn" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "SplitIt" })).toHaveAttribute("href", "/");
  });
});
