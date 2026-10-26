import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { EventError } from "../lib/events";
import {
  getInvitation,
  getInvitationAccess,
  invitationLoginPath,
  invitationPagePath,
  linkInvitationMember,
  listInvitationMembers,
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
      message: "Esa persona ya entró con otra cuenta. Elegí otro nombre de la lista.",
    });
  });

  it("arma la ruta de login que vuelve a la invitación", () => {
    expect(invitationPagePath(TOKEN)).toBe(`/eventos/invitacion/${TOKEN}`);
    expect(invitationLoginPath(TOKEN)).toBe(
      `/login?redirect=${encodeURIComponent(`/eventos/invitacion/${TOKEN}`)}`
    );
  });
});
