import { request } from "@/lib/api";
import { toEventError } from "@/lib/events";

export type MemberBalance = {
  memberId: number;
  memberName: string;
  netBalance: number;
};

const LIST_UNAVAILABLE = "No pudimos cargar los saldos. Probá de nuevo.";

export async function listBalances(eventId: number | string): Promise<MemberBalance[]> {
  try {
    return await request<MemberBalance[]>(`/api/events/${eventId}/balances`);
  } catch (err) {
    toEventError(err, LIST_UNAVAILABLE);
  }
}
