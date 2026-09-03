import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ProfilePage from "../app/(private)/perfil/page";
import { ProfileError } from "../lib/profile";

const replace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

vi.mock("@/lib/profile", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/profile")>();
  return {
    ...actual,
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
  };
});

import { getProfile, updateProfile } from "@/lib/profile";

const profile = { id: 1, name: "Ada Lovelace", email: "ada@example.com" };

describe("ProfilePage", () => {
  beforeEach(() => {
    replace.mockReset();
    vi.mocked(getProfile).mockReset();
    vi.mocked(updateProfile).mockReset();
  });

  it("muestra un estado de carga y luego el nombre, email e iniciales", async () => {
    vi.mocked(getProfile).mockResolvedValue(profile);
    render(<ProfilePage />);

    expect(screen.queryByText("Ada Lovelace")).not.toBeInTheDocument();

    expect(await screen.findByText("Ada Lovelace")).toBeInTheDocument();
    expect(screen.getByText("ada@example.com")).toBeInTheDocument();
    expect(screen.getByText("AL")).toBeInTheDocument();
  });

  it("redirige a /login si GET /me responde 401", async () => {
    vi.mocked(getProfile).mockRejectedValue(new ProfileError("unauthorized", "Tu sesión expiró."));
    render(<ProfilePage />);

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
    expect(screen.queryByText("Ada Lovelace")).not.toBeInTheDocument();
  });

  it("muestra un error con opción de reintentar ante un fallo de red", async () => {
    vi.mocked(getProfile).mockRejectedValue(
      new ProfileError("network", "No pudimos conectar con el servidor.")
    );
    render(<ProfilePage />);

    expect(await screen.findByText("No pudimos conectar con el servidor.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Reintentar" })).toBeInTheDocument();
  });

  it("precarga el formulario con los datos actuales al editar", async () => {
    vi.mocked(getProfile).mockResolvedValue(profile);
    const user = userEvent.setup();
    render(<ProfilePage />);

    await user.click(await screen.findByRole("button", { name: "Editar" }));

    expect(screen.getByLabelText("Nombre completo")).toHaveValue("Ada Lovelace");
    expect(screen.getByLabelText("Email")).toHaveValue("ada@example.com");
  });

  it("muestra errores de validación sin llegar a llamar al backend", async () => {
    vi.mocked(getProfile).mockResolvedValue(profile);
    const user = userEvent.setup();
    render(<ProfilePage />);

    await user.click(await screen.findByRole("button", { name: "Editar" }));
    await user.clear(screen.getByLabelText("Nombre completo"));
    await user.type(screen.getByLabelText("Nombre completo"), "   ");
    await user.click(screen.getByRole("button", { name: "Guardar cambios" }));

    expect(await screen.findByText("El nombre es obligatorio.")).toBeInTheDocument();
    expect(updateProfile).not.toHaveBeenCalled();
  });

  it("deshabilita 'Guardar cambios' durante el envío para evitar duplicados", async () => {
    let resolveUpdate: (value: typeof profile) => void = () => {};
    vi.mocked(getProfile).mockResolvedValue(profile);
    vi.mocked(updateProfile).mockReturnValueOnce(
      new Promise((resolve) => {
        resolveUpdate = resolve;
      })
    );
    const user = userEvent.setup();
    render(<ProfilePage />);

    await user.click(await screen.findByRole("button", { name: "Editar" }));
    const saveButton = screen.getByRole("button", { name: "Guardar cambios" });
    await user.click(saveButton);

    expect(saveButton).toBeDisabled();

    resolveUpdate({ ...profile, name: "Ada Byron Lovelace" });
    await waitFor(() => expect(screen.getByText("Ada Byron Lovelace")).toBeInTheDocument());
  });

  it("muestra el mensaje de email ya registrado junto al campo email", async () => {
    vi.mocked(getProfile).mockResolvedValue(profile);
    vi.mocked(updateProfile).mockRejectedValueOnce(
      new ProfileError("email-taken", "Ese email ya está registrado.", "email")
    );
    const user = userEvent.setup();
    render(<ProfilePage />);

    await user.click(await screen.findByRole("button", { name: "Editar" }));
    await user.click(screen.getByRole("button", { name: "Guardar cambios" }));

    const emailField = screen.getByLabelText("Email");
    expect(await screen.findByText("Ese email ya está registrado.")).toBeInTheDocument();
    expect(emailField).toHaveAttribute("aria-invalid", "true");
  });

  it("redirige a /login si PATCH /me responde 401", async () => {
    vi.mocked(getProfile).mockResolvedValue(profile);
    vi.mocked(updateProfile).mockRejectedValueOnce(
      new ProfileError("unauthorized", "Tu sesión expiró.")
    );
    const user = userEvent.setup();
    render(<ProfilePage />);

    await user.click(await screen.findByRole("button", { name: "Editar" }));
    await user.click(screen.getByRole("button", { name: "Guardar cambios" }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
  });

  it("mantiene los datos tipeados si falla el guardado por un error de servidor", async () => {
    vi.mocked(getProfile).mockResolvedValue(profile);
    vi.mocked(updateProfile).mockRejectedValueOnce(
      new ProfileError("server-error", "Ocurrió un error. Probá de nuevo.")
    );
    const user = userEvent.setup();
    render(<ProfilePage />);

    await user.click(await screen.findByRole("button", { name: "Editar" }));
    await user.clear(screen.getByLabelText("Nombre completo"));
    await user.type(screen.getByLabelText("Nombre completo"), "Ada Byron Lovelace");
    await user.click(screen.getByRole("button", { name: "Guardar cambios" }));

    expect(await screen.findByText("Ocurrió un error. Probá de nuevo.")).toBeInTheDocument();
    expect(screen.getByLabelText("Nombre completo")).toHaveValue("Ada Byron Lovelace");
  });
});
