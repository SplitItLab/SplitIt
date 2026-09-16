import { EventDetailView } from "@/components/event-detail-view";

export default async function EventDetailPage({ params }: PageProps<"/eventos/[id]">) {
  const { id } = await params;

  return (
    <div className="space-y-6 px-6 pt-4 pb-16 sm:px-10 sm:pt-6 lg:space-y-8">
      <EventDetailView id={id} />
    </div>
  );
}
