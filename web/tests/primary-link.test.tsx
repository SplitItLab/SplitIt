import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ArrowLeft } from "lucide-react";

import { PrimaryLink } from "@/components/primary-link";

describe("PrimaryLink", () => {
  it("renderiza un enlace con el destino y el texto", () => {
    render(<PrimaryLink href="/">Volver al inicio</PrimaryLink>);

    expect(screen.getByRole("link", { name: "Volver al inicio" })).toHaveAttribute("href", "/");
  });

  it("usa el tamaño mediano por defecto", () => {
    render(<PrimaryLink href="/">Volver al inicio</PrimaryLink>);

    expect(screen.getByRole("link")).toHaveClass("h-10", "text-xl");
  });

  it("aplica el tamaño chico cuando size es sm", () => {
    render(
      <PrimaryLink href="/" size="sm">
        Volver al inicio
      </PrimaryLink>
    );

    expect(screen.getByRole("link")).toHaveClass("h-8", "text-sm");
  });

  it("aplica el tamaño grande cuando size es lg", () => {
    render(
      <PrimaryLink href="/" size="lg">
        Volver al inicio
      </PrimaryLink>
    );

    expect(screen.getByRole("link")).toHaveClass("h-12", "text-2xl");
  });

  it("acepta un ícono junto al texto", () => {
    render(
      <PrimaryLink href="/">
        <ArrowLeft data-testid="icon" />
        Volver al inicio
      </PrimaryLink>
    );

    expect(screen.getByTestId("icon")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Volver al inicio" })).toBeInTheDocument();
  });
});
