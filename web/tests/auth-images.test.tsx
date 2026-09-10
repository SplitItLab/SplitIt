import { render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock("next/image", () => ({
  default: ({
    src,
    unoptimized,
    className,
  }: {
    src: string;
    unoptimized?: boolean;
    className?: string;
  }) => (
    // Test double for next/image.
    // eslint-disable-next-line @next/next/no-img-element
    <img src={src} data-unoptimized={unoptimized ? "true" : "false"} className={className} alt="" />
  ),
}));

import LoginPage from "../app/login/page";
import RegisterPage from "../app/register/page";

describe("imágenes de autenticación", () => {
  it("login sirve register-image.png sin el optimizador de Next", () => {
    const { container } = render(<LoginPage />);
    const image = container.querySelector("img");
    expect(image).toHaveAttribute("src", "/register-image.png");
    expect(image).toHaveAttribute("data-unoptimized", "true");
  });

  it("registro sirve register-image.png sin el optimizador de Next", () => {
    const { container } = render(<RegisterPage />);
    const image = container.querySelector("img");
    expect(image).toHaveAttribute("src", "/register-image.png");
    expect(image).toHaveAttribute("data-unoptimized", "true");
  });
});
