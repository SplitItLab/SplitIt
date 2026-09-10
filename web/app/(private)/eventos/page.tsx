"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { Plus, Search, X } from "lucide-react";

import { EventError, filterEventsByName, listEvents, type EventSummary } from "@/lib/events";
import { useSession } from "@/lib/use-session";
import { EventIcon } from "@/components/event-icon";
import { CreateEventDialog } from "@/components/create-event-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/ui/spinner";

type LoadState =
  | { status: "loading" }
  | { status: "unauthorized" }
  | { status: "error"; message: string }
  | { status: "loaded"; events: EventSummary[] };

export default function EventsPage() {
  const router = useRouter();
  const session = useSession();
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [query, setQuery] = useState("");
  const [creating, setCreating] = useState(false);

  const fetchEvents = useCallback(() => {
    listEvents()
      .then((events) => setState({ status: "loaded", events }))
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
              : "No pudimos cargar tus eventos. Probá de nuevo.",
        });
      });
  }, []);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  useEffect(() => {
    if (state.status === "unauthorized") router.replace("/login");
  }, [state.status, router]);

  const retry = () => {
    setState({ status: "loading" });
    fetchEvents();
  };

  const events = useMemo(() => (state.status === "loaded" ? state.events : []), [state]);
  const visibleEvents = useMemo(() => filterEventsByName(events, query), [events, query]);

  const hasEvents = events.length > 0;
  const searching = query.trim().length > 0;

  const handleCreated = (event: EventSummary) => {
    setState((current) =>
      current.status === "loaded"
        ? { status: "loaded", events: [event, ...current.events] }
        : { status: "loaded", events: [event] }
    );
    setQuery("");
  };

  return (
    <div className="space-y-6 px-6 pt-4 pb-16 sm:px-10 sm:pt-6 lg:space-y-8">
      <header>
        <h1 className="text-text-primary text-[40px] leading-[1.15] font-extrabold sm:text-[60px]">
          Tus eventos
        </h1>
        <p className="text-text-secondary mt-2 max-w-xl text-sm font-medium">
          {hasEvents
            ? "Seleccioná un evento creado para cargar gastos, revisar saldos y ver quién le debe a quién."
            : "Cuando tengas un evento creado para cargar gastos, revisar saldos y ver quien de lede a quien."}
        </p>
      </header>

      <section className="flex flex-col-reverse gap-3 sm:flex-row sm:items-center">
        {hasEvents && (
          <div className="relative flex-1">
            <Search
              aria-hidden="true"
              className="text-muted-foreground pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2"
            />
            <Input
              type="search"
              aria-label="Buscar evento"
              placeholder="Buscar evento"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              className="h-auto w-full rounded-[6px] py-2 pr-11 pl-9 text-base"
            />
            {searching && (
              <Button
                type="button"
                variant="ghost"
                size="icon"
                aria-label="Limpiar búsqueda"
                onClick={() => setQuery("")}
                className="absolute top-1/2 right-1.5 -translate-y-1/2 rounded-[6px]"
              >
                <X className="size-4" />
              </Button>
            )}
          </div>
        )}
        <Button
          type="button"
          onClick={() => setCreating(true)}
          className="h-10 w-full rounded-[8px] px-5 text-xl font-medium sm:w-auto"
        >
          <Plus className="size-5" />
          Crear evento
        </Button>
      </section>

      {state.status === "loading" && (
        <section className="flex items-center justify-center gap-2 py-16">
          <Spinner className="size-5" />
          <p className="text-text-secondary text-sm font-medium">Cargando tus eventos…</p>
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

      {state.status === "loaded" && !hasEvents && (
        <section className="border-border rounded-[24px] border p-6 sm:p-8">
          <div className="mx-auto max-w-sm text-center">
            <h2 className="text-text-primary text-2xl font-extrabold">Todavía no tenés eventos</h2>
            <p className="text-text-secondary mt-2 text-sm font-medium">
              Creá tu primer evento para empezar a dividir gastos con tu grupo.
            </p>
          </div>
        </section>
      )}

      {state.status === "loaded" && hasEvents && visibleEvents.length === 0 && (
        <section className="border-border rounded-[24px] border p-6 sm:p-8">
          <div className="mx-auto max-w-sm text-center">
            <span
              aria-hidden="true"
              className="bg-muted text-text-secondary mx-auto mb-3 flex size-12 items-center justify-center rounded-[16px]"
            >
              <Search className="size-5" />
            </span>
            <h2 className="text-text-primary text-2xl font-extrabold">Sin resultados</h2>
            <p className="text-text-secondary mt-2 text-sm font-medium">
              Ningún evento tuyo coincide con «{query.trim()}».
            </p>
          </div>
        </section>
      )}

      {state.status === "loaded" && visibleEvents.length > 0 && (
        <section className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
          {visibleEvents.map((event) => (
            <article
              key={event.id}
              className="border-border flex items-center gap-4 rounded-[24px] border p-4 sm:p-5"
            >
              <EventIcon iconKey={event.iconKey} />
              <div className="min-w-0 flex-1">
                <h2 className="text-text-primary truncate text-base font-extrabold">
                  {event.name}
                </h2>
                {event.description && (
                  <p className="text-text-secondary mt-1 line-clamp-2 text-sm font-medium">
                    {event.description}
                  </p>
                )}
                <p className="text-primary mt-2 text-sm font-medium">
                  {event.memberCount} integrantes · {event.baseCurrency}
                </p>
              </div>
            </article>
          ))}
        </section>
      )}

      <CreateEventDialog
        open={creating}
        onOpenChange={setCreating}
        onCreated={handleCreated}
        currentUserName={session.status === "authenticated" ? session.user.name : undefined}
      />
    </div>
  );
}
