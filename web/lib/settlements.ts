import { request } from "@/lib/api";
import { toEventError } from "@/lib/events";

export type SuggestedPaymentMember = {
  id: number;
  name: string;
};

export type SuggestedPayment = {
  fromMember: SuggestedPaymentMember;
  toMember: SuggestedPaymentMember;
  amount: number;
};

const LIST_UNAVAILABLE = "No pudimos cargar los pagos sugeridos. Probá de nuevo.";

export async function listSuggestedPayments(eventId: number | string): Promise<SuggestedPayment[]> {
  try {
    return await request<SuggestedPayment[]>(`/api/events/${eventId}/suggested-payments`);
  } catch (err) {
    toEventError(err, LIST_UNAVAILABLE);
  }
}
