import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { EventError } from "../lib/events";
import {
  enterAsGuest,
  getInvitation,
  getInvitationAccess,
  invitationLoginPath,
  invitationPagePath,
  linkInvitationMember,
  listInvitationMembers,
  recalledGuestMemberId,
  recalledInvitationPath,
  rememberGuestMember,
} from "../lib/invitations";

const TOKEN = "r7Qk2Vx9mLpZ4tHnCwB3sd";

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("invitaciones", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
  });

  it("pide los datos básicos del evento por token", async () => {
    const invitation = {
      name: "Juntada Asado",
      description: null,
      iconKey: "food",
      memberCount: 4,
    };
    vi.mocked(fetch).mockResolvedValue(jsonResponse(invitation));

    await expect(getInvitation(TOKEN)).resolves.toEqual(invitation);
    expect(fetch).toHaveBeenCalledWith(`/api/invitations/${TOKEN}`, expect.anything());
  });

  it("un token inválido se informa como evento no encontrado", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: "Event not found" }, 404));

    await expect(getInvitation("token-invalido")).rejects.toMatchObject({
      type: "not-found",
      message: "No encontramos este evento.",
    });
  });

  it("lista los integrantes marcando los ya vinculados", async () => {
    const members = [
      { id: 1, name: "Nicolas", isLinked: true },
      { id: 2, name: "Francisco", isLinked: false },
    ];
    vi.mocked(fetch).mockResolvedValue(jsonResponse(members));

    await expect(listInvitationMembers(TOKEN)).resolves.toEqual(members);
    expect(fetch).toHaveBeenCalledWith(`/api/invitations/${TOKEN}/members`, expect.anything());
  });

  it("pide el estado de acceso de la cuenta", async () => {
    const access = { eventId: 10, linkedMember: null };
    vi.mocked(fetch).mockResolvedValue(jsonResponse(access));

    await expect(getInvitationAccess(TOKEN)).resolves.toEqual(access);
    expect(fetch).toHaveBeenCalledWith(`/api/invitations/${TOKEN}/access`, expect.anything());
  });

  it("vincula la cuenta enviando el integrante elegido", async () => {
    const access = { eventId: 10, linkedMember: { id: 2, name: "Francisco" } };
    vi.mocked(fetch).mockResolvedValue(jsonResponse(access));

    await expect(linkInvitationMember(TOKEN, 2)).resolves.toEqual(access);
    expect(fetch).toHaveBeenCalledWith(
      `/api/invitations/${TOKEN}/link`,
      expect.objectContaining({ method: "POST", body: JSON.stringify({ memberId: 2 }) })
    );
  });

  it("explica el conflicto cuando el integrante ya fue tomado", async () => {
    vi.mocked(fetch).mockResolvedValue(
      jsonResponse({ message: "Member is already linked to an account" }, 409)
    );

    const error = await linkInvitationMember(TOKEN, 1).catch((err: unknown) => err);

    expect(error).toBeInstanceOf(EventError);
    expect(error).toMatchObject({
      type: "conflict",
      message: "No pudimos vincular tu cuenta. Elegí de nuevo.",
    });
  });

  it("entra como invitado enviando el integrante elegido", async () => {
    const guest = { type: "guest", eventId: 10, memberId: 2, name: "Francisco" };
    vi.mocked(fetch).mockResolvedValue(jsonResponse(guest));

    await expect(enterAsGuest(TOKEN, 2)).resolves.toEqual(guest);
    expect(fetch).toHaveBeenCalledWith(
      `/api/invite-links/${TOKEN}/guest-session`,
      expect.objectContaining({ method: "POST", body: JSON.stringify({ memberId: 2 }) })
    );
  });

  it("al entrar como invitado el dispositivo recuerda al integrante", async () => {
    vi.mocked(fetch).mockResolvedValue(
      jsonResponse({ type: "guest", eventId: 10, memberId: 2, name: "Francisco" })
    );

    await enterAsGuest(TOKEN, 2);

    expect(recalledGuestMemberId(TOKEN)).toBe(2);
  });

  it("al entrar como invitado el dispositivo recuerda la invitación de su evento", async () => {
    vi.mocked(fetch).mockResolvedValue(
      jsonResponse({ type: "guest", eventId: 10, memberId: 2, name: "Francisco" })
    );

    await enterAsGuest(TOKEN, 2);

    expect(recalledInvitationPath(10)).toBe(`/eventos/invitacion/${TOKEN}`);
    expect(recalledInvitationPath("10")).toBe(`/eventos/invitacion/${TOKEN}`);
  });

  it("no recuerda ninguna invitación de un evento al que no entró como invitado", () => {
    expect(recalledInvitationPath(10)).toBeNull();
  });

  it("explica que el integrante ya tiene cuenta cuando el backend responde 409", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: "Member has an account" }, 409));

    const error = await enterAsGuest(TOKEN, 1).catch((err: unknown) => err);

    expect(error).toBeInstanceOf(EventError);
    expect(error).toMatchObject({
      type: "conflict",
      message:
        "Esa persona ya tiene cuenta. Iniciá sesión con ella o elegí otro nombre de la lista.",
    });
    expect(recalledGuestMemberId(TOKEN)).toBeNull();
  });

  it("un link inválido al entrar como invitado se informa como no encontrado", async () => {
    vi.mocked(fetch).mockResolvedValue(jsonResponse({ message: "Invite link not found" }, 404));

    await expect(enterAsGuest("token-invalido", 1)).rejects.toMatchObject({ type: "not-found" });
  });

  it("sin conexión al entrar como invitado se informa como error de red", async () => {
    vi.mocked(fetch).mockRejectedValue(new TypeError("Failed to fetch"));

    await expect(enterAsGuest(TOKEN, 1)).rejects.toMatchObject({ type: "network" });
  });

  it("no recuerda a nadie si no se guardó nada", () => {
    expect(recalledGuestMemberId(TOKEN)).toBeNull();
  });

  it("ignora un valor guardado que no es un número", () => {
    localStorage.setItem(`splitit:guest-member:${TOKEN}`, "basura");

    expect(recalledGuestMemberId(TOKEN)).toBeNull();
  });

  it("no falla si el storage está bloqueado", () => {
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });

    expect(() => rememberGuestMember(TOKEN, 2)).not.toThrow();
    expect(recalledGuestMemberId(TOKEN)).toBeNull();
    expect(recalledInvitationPath(10)).toBeNull();
    vi.restoreAllMocks();
  });

  it("arma la ruta de login que vuelve a la invitación", () => {
    expect(invitationPagePath(TOKEN)).toBe(`/eventos/invitacion/${TOKEN}`);
    expect(invitationLoginPath(TOKEN)).toBe(
      `/login?redirect=${encodeURIComponent(`/eventos/invitacion/${TOKEN}`)}`
    );
  });
});
