import { request, ApiError } from "@/lib/api";
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
      throw new EventError(
        "conflict",
        "Esa persona ya entró con otra cuenta. Elegí otro nombre de la lista."
      );
    }
    toEventError(err);
  }
}

export function invitationPagePath(token: string): string {
  return `/eventos/invitacion/${encodeURIComponent(token)}`;
}

export function invitationLoginPath(token: string): string {
  return `/login?redirect=${encodeURIComponent(invitationPagePath(token))}`;
}
