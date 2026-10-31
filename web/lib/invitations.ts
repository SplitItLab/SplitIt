import { request, ApiError } from "@/lib/api";
import type { GuestSession } from "@/lib/auth";
import { EventError, toEventError } from "@/lib/events";

export type Invitation = {
  name: string;
  description?: string | null;
  iconKey?: string | null;
  memberCount: number;
};

export type InvitationMember = {
  id: number;
  name: string;
  isLinked: boolean;
};

export type InvitationAccess = {
  eventId: number;
  linkedMember: { id: number; name: string } | null;
};

function invitationPath(token: string, suffix = ""): string {
  return `/api/invitations/${encodeURIComponent(token)}${suffix}`;
}

export async function getInvitation(token: string): Promise<Invitation> {
  try {
    return await request<Invitation>(invitationPath(token));
  } catch (err) {
    toEventError(err);
  }
}

export async function listInvitationMembers(token: string): Promise<InvitationMember[]> {
  try {
    return await request<InvitationMember[]>(invitationPath(token, "/members"));
  } catch (err) {
    toEventError(err);
  }
}

export async function getInvitationAccess(token: string): Promise<InvitationAccess> {
  try {
    return await request<InvitationAccess>(invitationPath(token, "/access"));
  } catch (err) {
    toEventError(err);
  }
}

export async function linkInvitationMember(
  token: string,
  memberId: number
): Promise<InvitationAccess> {
  try {
    return await request<InvitationAccess>(invitationPath(token, "/link"), {
      method: "POST",
      body: JSON.stringify({ memberId }),
    });
  } catch (err) {
    if (err instanceof ApiError && err.status === 409) {
      throw new EventError("conflict", "No pudimos vincular tu cuenta. Elegí de nuevo.");
    }
    toEventError(err);
  }
}

export async function enterAsGuest(token: string, memberId: number): Promise<GuestSession> {
  try {
    const guest = await request<GuestSession>(
      `/api/invite-links/${encodeURIComponent(token)}/guest-session`,
      { method: "POST", body: JSON.stringify({ memberId }) }
    );
    rememberGuestMember(token, memberId);
    return guest;
  } catch (err) {
    if (err instanceof ApiError && err.status === 409) {
      throw new EventError(
        "conflict",
        "Esa persona ya tiene cuenta. Iniciá sesión con ella o elegí otro nombre de la lista."
      );
    }
    toEventError(err);
  }
}

// "Salir" borra la cookie de invitado, así que el dispositivo recuerda el integrante aparte.
function guestMemberKey(token: string): string {
  return `splitit:guest-member:${token}`;
}

export function rememberGuestMember(token: string, memberId: number): void {
  try {
    localStorage.setItem(guestMemberKey(token), String(memberId));
  } catch {
    // Sin storage (modo privado, bloqueado): el selector simplemente no preselecciona.
  }
}

export function recalledGuestMemberId(token: string): number | null {
  try {
    const raw = localStorage.getItem(guestMemberKey(token));
    const memberId = raw === null ? Number.NaN : Number(raw);
    return Number.isInteger(memberId) ? memberId : null;
  } catch {
    return null;
  }
}

export function invitationPagePath(token: string): string {
  return `/eventos/invitacion/${encodeURIComponent(token)}`;
}

export function invitationLoginPath(token: string): string {
  return `/login?redirect=${encodeURIComponent(invitationPagePath(token))}`;
}
