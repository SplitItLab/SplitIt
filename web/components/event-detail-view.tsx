"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Pencil, Trash2, UserPlus } from "lucide-react";

import { EventError, getEventById, type EventDetail, type EventSummary } from "@/lib/events";
import { showAppToast } from "@/lib/toast";
import { EventNotFound } from "@/components/event-not-found";
import { EventDetailContent } from "@/components/event-detail-content";
import { EditEventDialog } from "@/components/edit-event-dialog";
import { DeleteEventDialog } from "@/components/delete-event-dialog";
import { InviteEventDialog } from "@/components/invite-event-dialog";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/ui/spinner";

type LoadState =
  | { status: "loading" }
  | { status: "unauthorized" }
  | { status: "not-found" }
  | { status: "error"; message: string }
  | { status: "loaded"; event: EventDetail };

export function EventDetailView({ id }: { id: string }) {
  const router = useRouter();
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [inviting, setInviting] = useState(false);

  const fetchEvent = useCallback(() => {
    getEventById(id)
      .then((event) => setState({ status: "loaded", event }))
      .catch((err) => {
        if (err instanceof EventError && err.type === "unauthorized") {
          setState({ status: "unauthorized" });
          return;
        }
        if (err instanceof EventError && (err.type === "forbidden" || err.type === "not-found")) {
          setState({ status: "not-found" });
          return;
        }
        setState({
          status: "error",
          message:
            err instanceof EventError
              ? err.message
              : "No pudimos cargar el evento. Probá de nuevo.",
        });
      });
  }, [id]);

  useEffect(() => {
    fetchEvent();
  }, [fetchEvent]);

  useEffect(() => {
    if (state.status === "unauthorized") router.replace("/login");
  }, [state.status, router]);

  const retry = () => {
    setState({ status: "loading" });
    fetchEvent();
  };

  const handleDeleted = () => {
    if (state.status !== "loaded") return;
    showAppToast("success", `El evento «${state.event.name}» fue eliminado`);
    router.push("/eventos");
  };

  const handleUpdated = (updated: EventSummary) => {
    setState((current) =>
      current.status === "loaded"
        ? { status: "loaded", event: { ...current.event, ...updated } }
        : current
    );
  };

  const handleUnauthorized = useCallback(() => {
    setState({ status: "unauthorized" });
  }, []);

  if (state.status === "not-found") {
    return <EventNotFound />;
  }

  if (state.status === "loaded") {
    return (
      <>
        <div className="flex items-center justify-between">
          <Link
            href="/eventos"
            aria-label="Volver a eventos"
            className="border-border text-text-secondary hover:text-text-primary hover:bg-muted focus-visible:ring-ring/50 inline-flex size-12 items-center justify-center rounded-full border outline-none focus-visible:ring-3"
          >
            <ArrowLeft className="size-5" />
          </Link>

          {state.event.isOwner && (
            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="ghost"
                size="icon"
                aria-label="Invitar"
                onClick={() => setInviting(true)}
                className="size-11 rounded-full"
              >
                <UserPlus className="size-5" />
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                aria-label="Editar evento"
                onClick={() => setEditing(true)}
                className="size-11 rounded-full"
              >
                <Pencil className="size-5" />
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                aria-label="Eliminar evento"
                onClick={() => setDeleting(true)}
                className="hover:text-destructive size-11 rounded-full"
              >
                <Trash2 className="size-5" />
              </Button>
            </div>
          )}
        </div>

        <EventDetailContent event={state.event} />

        <InviteEventDialog
          open={inviting}
          onOpenChange={setInviting}
          event={state.event}
          onUnauthorized={handleUnauthorized}
        />
        <EditEventDialog
          open={editing}
          onOpenChange={setEditing}
          event={state.event}
          onUpdated={handleUpdated}
          onUnauthorized={() => setState({ status: "unauthorized" })}
        />
        <DeleteEventDialog
          open={deleting}
          onOpenChange={setDeleting}
          event={state.event}
          onDeleted={handleDeleted}
          onUnauthorized={() => setState({ status: "unauthorized" })}
        />
      </>
    );
  }

  return (
    <>
      <Link
        href="/eventos"
        className="text-text-secondary hover:text-text-primary inline-flex items-center gap-2 text-sm font-medium"
      >
        <ArrowLeft className="size-4" />
        Volver a eventos
      </Link>

      {state.status === "loading" && (
        <section className="flex items-center justify-center gap-2 py-16">
          <Spinner className="size-5" />
          <p className="text-text-secondary text-sm font-medium">Cargando el evento…</p>
        </section>
      )}

      {state.status === "error" && (
        <section className="flex flex-col items-start gap-3">
          <Alert variant="destructive">
            <AlertDescription>{state.message}</AlertDescription>
          </Alert>
          <Button type="button" variant="outline" onClick={retry}>
            Reintentar
          </Button>
        </section>
      )}
    </>
  );
}
