"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";

import { EventError, getEvent, type EventDetail } from "@/lib/events";
import { EventNotFound } from "@/components/event-not-found";
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

  const fetchEvent = useCallback(() => {
    getEvent(id)
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

  if (state.status === "not-found") {
    return <EventNotFound />;
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

      {state.status === "loaded" && (
        <>
          <header>
            <p className="text-primary text-sm font-medium">Evento #{id}</p>
            <h1 className="text-text-primary mt-1 text-[40px] leading-[1.15] font-extrabold sm:text-[60px]">
              Detalle del evento
            </h1>
            <p className="text-text-secondary mt-2 max-w-xl text-sm font-medium">
              Administrá los gastos del grupo y revisá los saldos de este evento.
            </p>
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
        </>
      )}
    </>
  );
}
