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
  };
});

import { EventError } from "@/lib/events";
import {
  getInvitation,
  getInvitationAccess,
  linkInvitationMember,
  listInvitationMembers,
  type Invitation,
  type InvitationMember,
} from "@/lib/invitations";
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

  it("explica el conflicto y recarga la lista si el integrante ya fue tomado", async () => {
    signedIn();
    vi.mocked(linkInvitationMember).mockRejectedValue(
      new EventError(
        "conflict",
        "Esa persona ya entró con otra cuenta. Elegí otro nombre de la lista."
      )
    );
    const user = userEvent.setup();
    render(<EventInvitationView token={TOKEN} />);

    await user.selectOptions(
      await screen.findByLabelText("¿Quién sos en este evento?"),
      "Francisco"
    );
    await user.click(screen.getByRole("button", { name: "Soy esta persona" }));

    expect(
      await screen.findByText(
        "Esa persona ya entró con otra cuenta. Elegí otro nombre de la lista."
      )
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
});
