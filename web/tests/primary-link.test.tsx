import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ArrowLeft } from "lucide-react";

import { PrimaryLink } from "@/components/primary-link";

describe("PrimaryLink", () => {
  it("renderiza un enlace con el destino y el texto", () => {
    render(<PrimaryLink href="/">Volver al inicio</PrimaryLink>);

    expect(screen.getByRole("link", { name: "Volver al inicio" })).toHaveAttribute("href", "/");
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
