"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ArrowRight, Check } from "lucide-react";

import { EventError, type EventDetail } from "@/lib/events";
import { formatMoney, listExpenses } from "@/lib/expenses";
import { getInitials } from "@/lib/profile";
import { listSuggestedPayments, type SuggestedPayment } from "@/lib/settlements";
import { LoadingIndicator } from "@/components/loading-indicator";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";

type LoadState =
  | { status: "loading" }
  | { status: "ready"; hasExpenses: boolean; payments: SuggestedPayment[] }
  | { status: "error"; message: string };

export function EventSuggestedPayments({
  event,
  onUnauthorized,
  onNotFound,
}: {
  event: EventDetail;
  onUnauthorized: () => void;
  onNotFound: () => void;
}) {
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const requestId = useRef(0);

  const load = useCallback(() => {
    const current = ++requestId.current;
    Promise.all([listExpenses(event.id), listSuggestedPayments(event.id)])
      .then(([expenses, payments]) => {
        if (current !== requestId.current) return;
        setState({ status: "ready", hasExpenses: expenses.length > 0, payments });
      })
      .catch((err: unknown) => {
        if (current !== requestId.current) return;
        if (err instanceof EventError && err.type === "unauthorized") {
          onUnauthorized();
          return;
        }
        if (err instanceof EventError && (err.type === "forbidden" || err.type === "not-found")) {
          onNotFound();
          return;
        }
        setState({
          status: "error",
          message:
            err instanceof EventError
              ? err.message
              : "No pudimos cargar los pagos sugeridos. Probá de nuevo.",
        });
      });
  }, [event.id, onNotFound, onUnauthorized]);

  useEffect(() => {
    load();
  }, [load]);

  const retry = () => {
    setState({ status: "loading" });
    load();
  };

  if (state.status === "loading") {
    return (
      <section className="flex items-center justify-center gap-2 py-16">
        <LoadingIndicator
          message="Cargando los pagos sugeridos…"
          spinnerClassName="size-5"
          className="text-text-secondary text-sm font-medium"
        />
      </section>
    );
  }

  if (state.status === "error") {
    return (
      <section className="flex flex-col items-start gap-3">
        <Alert variant="destructive">
          <AlertDescription>{state.message}</AlertDescription>
        </Alert>
        <Button type="button" variant="outline" onClick={retry}>
          Reintentar
        </Button>
      </section>
    );
  }

  return (
    <section className="space-y-3">
      <h3 className="text-text-primary text-base font-extrabold">Pagos sugeridos</h3>
      {!state.hasExpenses ? (
        <div className="border-border rounded-[24px] border p-6 text-center">
          <p className="text-text-primary font-extrabold">Todavía no hay pagos sugeridos</p>
          <p className="text-text-secondary text-sm font-medium">
            Cuando el evento tenga gastos cargados, acá vas a ver quién le tiene que pagar a quién.
          </p>
        </div>
      ) : state.payments.length > 0 ? (
        <ul className="grid gap-3 xl:grid-cols-2 [&>*]:min-w-0">
          {state.payments.map((payment) => (
            <SuggestedPaymentCard
              key={`${payment.fromMember.id}-${payment.toMember.id}`}
              payment={payment}
              currency={event.baseCurrency}
            />
          ))}
        </ul>
      ) : (
        <div className="border-border rounded-[24px] border p-6 text-center">
          <Check aria-hidden="true" className="text-primary mx-auto size-9" />
          <p className="text-text-primary mt-3 font-extrabold">Está todo claro</p>
          <p className="text-text-secondary text-sm font-medium">No hay pagos pendientes.</p>
        </div>
      )}
    </section>
  );
}

function SuggestedPaymentCard({
  payment,
  currency,
}: {
  payment: SuggestedPayment;
  currency: string;
}) {
  return (
    <li>
      <article
        aria-label={`${payment.fromMember.name} le paga a ${payment.toMember.name}`}
        className="border-border flex items-center gap-2 rounded-[24px] border p-4 sm:gap-3"
      >
        <PaymentParty name={payment.fromMember.name} role="debe pagar" tone="owes" />
        <ArrowRight aria-hidden="true" className="text-primary size-4 shrink-0 sm:size-5" />
        <PaymentParty name={payment.toMember.name} role="debe recibir" tone="receives" />
        <p className="text-primary shrink-0 text-sm font-extrabold">
          {formatMoney(payment.amount, currency)}
        </p>
      </article>
    </li>
  );
}

function PaymentParty({
  name,
  role,
  tone,
}: {
  name: string;
  role: string;
  tone: "owes" | "receives";
}) {
  return (
    <>
      <Avatar className="size-8 sm:size-10">
        <AvatarFallback
          className={
            tone === "owes" ? "bg-destructive/10 text-destructive" : "bg-primary/10 text-primary"
          }
        >
          {getInitials(name)}
        </AvatarFallback>
      </Avatar>
      <div className="min-w-0 flex-1">
        <p className="text-text-primary truncate text-sm font-extrabold">{name}</p>
        <p className="text-text-secondary hidden text-xs font-medium sm:block">{role}</p>
      </div>
    </>
  );
}
