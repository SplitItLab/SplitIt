import { EventInvitationView } from "@/components/event-invitation-view";

export default async function EventInvitationPage({
  params,
}: PageProps<"/eventos/invitacion/[token]">) {
  const { token } = await params;

  return <EventInvitationView token={token} />;
}
