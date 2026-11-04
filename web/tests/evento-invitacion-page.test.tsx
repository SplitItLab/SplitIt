import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor, cleanup } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

const push = vi.fn();
const replace = vi.fn();
// Como el router real de Next, el mock devuelve siempre la misma instancia.
const router = { push, replace };
vi.mock("next/navigation", () => ({
  useRouter: () => router,
}));

vi.mock("next/image", () => ({
  // Test double for next/image.
  // eslint-disable-next-line @next/next/no-img-element
  default: ({ src }: { src: string }) => <img src={src} alt="" />,
}));

vi.mock("@/lib/use-session", () => ({ useSession: vi.fn() }));

vi.mock("@/lib/toast", () => ({ showAppToast: vi.fn() }));

vi.mock("@/lib/invitations", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/invitations")>();
  return {
    ...actual,
    getInvitation: vi.fn(),
    listInvitationMembers: vi.fn(),
    getInvitationAccess: vi.fn(),
    linkInvitationMember: vi.fn(),
    enterAsGuest: vi.fn(),
  };
});

import { EventError } from "@/lib/events";
import {
  enterAsGuest,
  getInvitation,
  getInvitationAccess,
  linkInvitationMember,
  listInvitationMembers,
  type Invitation,
  type InvitationMember,
} from "@/lib/invitations";
import { showAppToast } from "@/lib/toast";
import { useSession } from "@/lib/use-session";
import { EventInvitationView } from "../components/event-invitation-view";

const TOKEN = "r7Qk2Vx9mLpZ4tHnCwB3sd";

const invitation: Invitation = {
  name: "Juntada Asado",
  description: "Asado del sábado en casa de Nicolas",
  iconKey: "food",
  memberCount: 3,
};

const members: InvitationMember[] = [
  { id: 1, name: "Nicolas", isLinked: true },
  { id: 2, name: "Francisco", isLinked: false },
  { id: 3, name: "Lucas", isLinked: false },
];

const sofia = { id: 7, name: "Sofia", email: "sofia@email.com" };

const francisco = { type: "guest" as const, eventId: 10, memberId: 2, name: "Francisco" };

function asGuest(guest = francisco) {
  vi.mocked(useSession).mockReturnValue({ status: "guest", guest });
}

function signedIn() {
  vi.mocked(useSession).mockReturnValue({ status: "authenticated", user: sofia });
}

describe("EventInvitationView", () => {
  beforeEach(() => {
    push.mockClear();
    replace.mockClear();
    vi.mocked(getInvitation).mockReset();
    vi.mocked(getInvitation).mockResolvedValue(invitation);
    vi.mocked(listInvitationMembers).mockReset();
    vi.mocked(listInvitationMembers).mockResolvedValue(members);
    vi.mocked(getInvitationAccess).mockReset();
    vi.mocked(getInvitationAccess).mockResolvedValue({ eventId: 10, linkedMember: null });
    vi.mocked(linkInvitationMember).mockReset();
    vi.mocked(enterAsGuest).mockReset();
    vi.mocked(showAppToast).mockClear();
    localStorage.clear();
  });

  afterEach(() => {
    cleanup();
  });

  it("muestra los datos básicos del evento con un enlace válido", async () => {
    vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
    render(<EventInvitationView token={TOKEN} />);

    expect(await screen.findByRole("heading", { name: "Juntada Asado" })).toBeInTheDocument();
    expect(screen.getByText("Asado del sábado en casa de Nicolas")).toBeInTheDocument();
    expect(screen.getByText("participantes")).toBeInTheDocument();
    expect(getInvitation).toHaveBeenCalledWith(TOKEN);
  });

  it("muestra el mensaje genérico cuando el enlace es inválido o el evento no existe", async () => {
    vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
    vi.mocked(getInvitation).mockRejectedValue(
      new EventError("not-found", "No encontramos este evento.")
    );
    render(<EventInvitationView token="token-invalido" />);

    expect(
      await screen.findByRole("heading", { name: "No encontramos este evento" })
    ).toBeInTheDocument();
  });

  it("sin sesión, continuar con cuenta lleva al login y vuelve a la invitación", async () => {
    vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
    render(<EventInvitationView token={TOKEN} />);

    const link = await screen.findByRole("link", { name: "Continuar con mi cuenta" });
    expect(link).toHaveAttribute(
      "href",
      `/login?redirect=${encodeURIComponent(`/eventos/invitacion/${TOKEN}`)}`
    );
    expect(getInvitationAccess).not.toHaveBeenCalled();
  });

  it("con sesión, muestra a los ya vinculados pero no deja elegirlos", async () => {
    signedIn();
    render(<EventInvitationView token={TOKEN} />);

    const taken = await screen.findByRole("option", { name: /Nicolas/ });
    expect(taken).toBeDisabled();
    expect(taken).toHaveTextContent("ya entró con su cuenta");
    expect(screen.getByRole("option", { name: "Francisco" })).toBeEnabled();
    expect(screen.getByText(/Entrás con la cuenta de sofia@email.com/)).toBeInTheDocument();
  });

  it("avisa cuando no queda ningún integrante disponible sin ocultar la lista", async () => {
    signedIn();
    vi.mocked(listInvitationMembers).mockResolvedValue(
      members.map((member) => ({ ...member, isLinked: true }))
    );
    render(<EventInvitationView token={TOKEN} />);

    expect(
      await screen.findByText("Todos los integrantes ya tienen una cuenta vinculada")
    ).toBeInTheDocument();
    expect(screen.getByRole("option", { name: /Francisco/ })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Soy esta persona" })).toBeDisabled();
  });

  it("no muestra el aviso mientras quede algún integrante disponible", async () => {
    signedIn();
    render(<EventInvitationView token={TOKEN} />);

    await screen.findByRole("option", { name: "Francisco" });
    expect(
      screen.queryByText("Todos los integrantes ya tienen una cuenta vinculada")
    ).not.toBeInTheDocument();
  });

  it("vincularse deja al usuario en el evento", async () => {
    signedIn();
    vi.mocked(linkInvitationMember).mockResolvedValue({
      eventId: 10,
      linkedMember: { id: 2, name: "Francisco" },
    });
    const user = userEvent.setup();
    render(<EventInvitationView token={TOKEN} />);

    await user.selectOptions(
      await screen.findByLabelText("¿Quién sos en este evento?"),
      "Francisco"
    );
    await user.click(screen.getByRole("button", { name: "Soy esta persona" }));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/eventos/10"));
    expect(linkInvitationMember).toHaveBeenCalledWith(TOKEN, 2);
  });

  it("pide elegir a alguien antes de vincular", async () => {
    signedIn();
    const user = userEvent.setup();
    render(<EventInvitationView token={TOKEN} />);

    await user.click(await screen.findByRole("button", { name: "Soy esta persona" }));

    expect(await screen.findByText("Elegí quién sos de la lista.")).toBeInTheDocument();
    expect(linkInvitationMember).not.toHaveBeenCalled();
  });

  it("si la vinculación falla, pide elegir de nuevo y actualiza la lista", async () => {
    signedIn();
    vi.mocked(linkInvitationMember).mockRejectedValue(
      new EventError("conflict", "No pudimos vincular tu cuenta. Elegí de nuevo.")
    );
    const user = userEvent.setup();
    render(<EventInvitationView token={TOKEN} />);

    await user.selectOptions(
      await screen.findByLabelText("¿Quién sos en este evento?"),
      "Francisco"
    );
    await user.click(screen.getByRole("button", { name: "Soy esta persona" }));

    expect(
      await screen.findByText("No pudimos vincular tu cuenta. Elegí de nuevo.")
    ).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
    await waitFor(() => expect(listInvitationMembers).toHaveBeenCalledTimes(2));
  });

  it("si la cuenta ya estaba vinculada, no pregunta de nuevo y ofrece entrar al evento", async () => {
    signedIn();
    vi.mocked(getInvitationAccess).mockResolvedValue({
      eventId: 10,
      linkedMember: { id: 2, name: "Francisco" },
    });
    render(<EventInvitationView token={TOKEN} />);

    expect(await screen.findByText(/Ya estás en este evento como/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Entrar al evento" })).toHaveAttribute(
      "href",
      "/eventos/10"
    );
    expect(screen.queryByLabelText("¿Quién sos en este evento?")).not.toBeInTheDocument();
  });

  it("vuelve al login si la sesión venció al cargar los integrantes", async () => {
    signedIn();
    vi.mocked(getInvitationAccess).mockRejectedValue(
      new EventError("unauthorized", "Tu sesión expiró. Iniciá sesión de nuevo.")
    );
    render(<EventInvitationView token={TOKEN} />);

    await waitFor(() =>
      expect(replace).toHaveBeenCalledWith(
        `/login?redirect=${encodeURIComponent(`/eventos/invitacion/${TOKEN}`)}`
      )
    );
  });

  describe("sin cuenta", () => {
    async function openGuestForm() {
      const user = userEvent.setup();
      render(<EventInvitationView token={TOKEN} />);
      await user.click(await screen.findByRole("button", { name: "Continuar sin cuenta" }));
      return user;
    }

    it("sin sesión ofrece continuar sin cuenta además de continuar con cuenta", async () => {
      vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
      render(<EventInvitationView token={TOKEN} />);

      expect(await screen.findByRole("button", { name: "Continuar sin cuenta" })).toBeEnabled();
      expect(screen.getByRole("link", { name: "Continuar con mi cuenta" })).toBeInTheDocument();
      expect(listInvitationMembers).not.toHaveBeenCalled();
    });

    it("elegir un integrante y entrar crea la sesión de invitado y va al evento", async () => {
      vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
      vi.mocked(enterAsGuest).mockResolvedValue(francisco);
      const user = await openGuestForm();

      await user.selectOptions(
        await screen.findByLabelText("¿Quién sos en este evento?"),
        "Francisco"
      );
      await user.click(screen.getByRole("button", { name: "Entrar como invitado" }));

      await waitFor(() => expect(push).toHaveBeenCalledWith("/eventos/10"));
      expect(enterAsGuest).toHaveBeenCalledWith(TOKEN, 2);
      expect(showAppToast).toHaveBeenCalledWith("success", "Entraste al evento como Francisco");
    });

    it("exige elegir un integrante", async () => {
      vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
      const user = await openGuestForm();

      await user.click(await screen.findByRole("button", { name: "Entrar como invitado" }));

      expect(await screen.findByText("Elegí quién sos de la lista.")).toBeInTheDocument();
      expect(enterAsGuest).not.toHaveBeenCalled();
    });

    it("no permite elegir a los integrantes que ya tienen cuenta", async () => {
      vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
      await openGuestForm();

      const taken = await screen.findByRole("option", { name: /Nicolas/ });
      expect(taken).toBeDisabled();
      expect(taken).toHaveTextContent("ya entró con su cuenta");
    });

    it("avisa y no deja entrar cuando todos los integrantes ya tienen cuenta", async () => {
      vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
      vi.mocked(listInvitationMembers).mockResolvedValue(
        members.map((member) => ({ ...member, isLinked: true }))
      );
      await openGuestForm();

      expect(
        await screen.findByText(/Todos los integrantes ya tienen una cuenta/)
      ).toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Entrar como invitado" })).toBeDisabled();
    });

    it("explica que el integrante tiene cuenta, recarga la lista y limpia la selección", async () => {
      vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
      vi.mocked(enterAsGuest).mockRejectedValue(
        new EventError("conflict", "Esa persona ya tiene cuenta. Iniciá sesión con ella.")
      );
      const user = await openGuestForm();

      const select = await screen.findByLabelText("¿Quién sos en este evento?");
      await user.selectOptions(select, "Francisco");
      await user.click(screen.getByRole("button", { name: "Entrar como invitado" }));

      expect(
        await screen.findByText("Esa persona ya tiene cuenta. Iniciá sesión con ella.")
      ).toBeInTheDocument();
      expect(push).not.toHaveBeenCalled();
      await waitFor(() => expect(listInvitationMembers).toHaveBeenCalledTimes(2));
      expect(await screen.findByLabelText("¿Quién sos en este evento?")).toHaveValue("");
    });

    it("muestra un mensaje claro sin conexión y permite reintentar", async () => {
      vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
      vi.mocked(listInvitationMembers).mockRejectedValueOnce(
        new EventError("network", "No pudimos conectar con el servidor.")
      );
      const user = await openGuestForm();

      expect(await screen.findByText("No pudimos conectar con el servidor.")).toBeInTheDocument();
      await user.click(screen.getByRole("button", { name: "Reintentar" }));

      expect(await screen.findByLabelText("¿Quién sos en este evento?")).toBeInTheDocument();
    });

    it("preselecciona al integrante que el dispositivo recuerda", async () => {
      vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
      localStorage.setItem(`splitit:guest-member:${TOKEN}`, "3");
      render(<EventInvitationView token={TOKEN} />);

      expect(await screen.findByLabelText("¿Quién sos en este evento?")).toHaveValue("3");
    });

    it("ignora un integrante recordado que ya tiene cuenta o no existe", async () => {
      vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
      localStorage.setItem(`splitit:guest-member:${TOKEN}`, "1");
      render(<EventInvitationView token={TOKEN} />);
      expect(await screen.findByLabelText("¿Quién sos en este evento?")).toHaveValue("");

      cleanup();
      localStorage.setItem(`splitit:guest-member:${TOKEN}`, "99");
      render(<EventInvitationView token={TOKEN} />);
      expect(await screen.findByLabelText("¿Quién sos en este evento?")).toHaveValue("");
    });

    it("si el integrante que el dispositivo recuerda se vinculó a una cuenta, avisa y deja elegir a otro", async () => {
      vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
      localStorage.setItem(`splitit:guest-member:${TOKEN}`, "1");
      render(<EventInvitationView token={TOKEN} />);

      expect(await screen.findByText(/ya está vinculado a una cuenta/)).toHaveTextContent(
        "Nicolas ya está vinculado a una cuenta"
      );
      expect(screen.getByRole("option", { name: "Francisco" })).toBeEnabled();
      expect(screen.getByRole("button", { name: "Entrar como invitado" })).toBeEnabled();
    });

    it("no avisa si el integrante que el dispositivo recuerda sigue disponible", async () => {
      vi.mocked(useSession).mockReturnValue({ status: "unauthenticated" });
      localStorage.setItem(`splitit:guest-member:${TOKEN}`, "3");
      render(<EventInvitationView token={TOKEN} />);

      await screen.findByLabelText("¿Quién sos en este evento?");
      expect(screen.queryByText(/ya está vinculado a una cuenta/)).not.toBeInTheDocument();
    });

    it("con sesión de invitado del mismo evento ofrece entrar directo", async () => {
      asGuest();
      render(<EventInvitationView token={TOKEN} />);

      expect(await screen.findByText(/Ya elegiste ser/)).toHaveTextContent("Francisco");
      expect(screen.getByRole("link", { name: "Entrar al evento" })).toHaveAttribute(
        "href",
        "/eventos/10"
      );
      expect(screen.queryByLabelText("¿Quién sos en este evento?")).not.toBeInTheDocument();
    });

    it("«No soy yo» permite elegir a otra persona", async () => {
      asGuest();
      const user = userEvent.setup();
      render(<EventInvitationView token={TOKEN} />);

      await user.click(await screen.findByRole("button", { name: "No soy yo" }));

      expect(await screen.findByLabelText("¿Quién sos en este evento?")).toBeInTheDocument();
    });

    it("ante una sesión de invitado de otro evento, primero ofrece continuar sin cuenta", async () => {
      asGuest({ ...francisco, eventId: 77, memberId: 500 });
      const user = userEvent.setup();
      render(<EventInvitationView token={TOKEN} />);

      // Elegir acá reemplaza la cookie y le quita el acceso al otro evento: no se abre solo.
      await user.click(await screen.findByRole("button", { name: "Continuar sin cuenta" }));

      expect(await screen.findByLabelText("¿Quién sos en este evento?")).toBeInTheDocument();
      expect(screen.queryByText(/Ya elegiste ser/)).not.toBeInTheDocument();
    });

    it("ante una sesión de invitado de otro evento no muestra el selector sin que lo pida", async () => {
      asGuest({ ...francisco, eventId: 77, memberId: 500 });
      render(<EventInvitationView token={TOKEN} />);

      expect(await screen.findByRole("button", { name: "Continuar sin cuenta" })).toBeEnabled();
      expect(screen.queryByLabelText("¿Quién sos en este evento?")).not.toBeInTheDocument();
    });
  });
});
