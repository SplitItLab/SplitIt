"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Pencil, Trash2 } from "lucide-react";

import {
  currencyLabel,
  EventError,
  getEventById,
  type EventDetail,
  type EventSummary,
} from "@/lib/events";
import { EventIcon } from "@/components/event-icon";
import { EditEventDialog } from "@/components/edit-event-dialog";
import { DeleteEventDialog } from "@/components/delete-event-dialog";
import { showAppToast } from "@/lib/toast";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/ui/spinner";

type LoadState =
  | { status: "loading" }
  | { status: "unauthorized" }
  | { status: "error"; message: string }
  | { status: "loaded"; event: EventDetail };

export default function EventDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const fetchEvent = useCallback(() => {
    getEventById(params.id)
      .then((event) => setState({ status: "loaded", event }))
      .catch((err) => {
        if (err instanceof EventError && err.type === "unauthorized") {
          setState({ status: "unauthorized" });
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
  }, [params.id]);

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

  return (
    <div className="space-y-6 px-6 pt-4 pb-16 sm:px-10 sm:pt-6 lg:space-y-8">
      <div className="flex items-center justify-between">
        <Link
          href="/eventos"
          className="text-text-secondary hover:text-text-primary inline-flex items-center gap-2 text-sm font-medium"
        >
          <ArrowLeft className="size-4" />
          Volver a eventos
        </Link>
        {state.status === "loaded" && state.event.isOwner && (
          <div className="flex items-center gap-2">
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

      {state.status === "loaded" && (
        <>
          <header className="flex items-center gap-4">
            <EventIcon iconKey={state.event.iconKey} />
            <div className="min-w-0">
              <h1 className="text-text-primary truncate text-[40px] leading-[1.15] font-extrabold sm:text-[60px]">
                {state.event.name}
              </h1>
              {state.event.description && (
                <p className="text-text-secondary mt-2 max-w-xl text-sm font-medium">
                  {state.event.description}
                </p>
              )}
              <p className="text-primary mt-2 text-sm font-medium">
                {state.event.memberCount} integrantes · {currencyLabel(state.event.baseCurrency)}
              </p>
            </div>
          </header>

          <div className="grid gap-4 lg:grid-cols-2">
            <section className="border-border rounded-[24px] border p-6">
              <h2 className="text-text-primary text-2xl font-extrabold">Gastos</h2>
              <p className="text-text-secondary mt-2 text-sm font-medium">
                Los gastos del evento se mostrarán acá.
              </p>
            </section>
            <section className="border-border rounded-[24px] border p-6">
              <h2 className="text-text-primary text-2xl font-extrabold">Saldos</h2>
              <p className="text-text-secondary mt-2 text-sm font-medium">
                Los saldos entre integrantes se mostrarán acá.
              </p>
            </section>
          </div>

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
      )}
    </div>
  );
}
