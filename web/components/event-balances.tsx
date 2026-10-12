"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import { EventError, type EventDetail } from "@/lib/events";
import { formatMoney } from "@/lib/expenses";
import { listBalances, type MemberBalance } from "@/lib/balances";
import { getInitials } from "@/lib/profile";
import { LoadingIndicator } from "@/components/loading-indicator";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";

type LoadState =
  | { status: "loading" }
  | { status: "ready"; balances: MemberBalance[] }
  | { status: "error"; message: string };

export function EventBalances({
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
    listBalances(event.id)
      .then((balances) => {
        if (current !== requestId.current) return;
        setState({ status: "ready", balances });
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
              : "No pudimos cargar los saldos. Probá de nuevo.",
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

  return (
    <div className="space-y-4">
      <h2 className="text-text-primary text-2xl font-extrabold">Saldos</h2>

      {state.status === "loading" && (
        <section className="flex items-center justify-center gap-2 py-16">
          <LoadingIndicator
            message="Cargando los saldos…"
            spinnerClassName="size-5"
            className="text-text-secondary text-sm font-medium"
          />
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

      {state.status === "ready" &&
        (state.balances.every((balance) => balance.netBalance === 0) ? (
          <div className="border-border rounded-[24px] border p-6 sm:p-8">
            <div className="mx-auto max-w-sm text-center">
              <h3 className="text-text-primary text-2xl font-extrabold">Todavía no hay saldos</h3>
              <p className="text-text-secondary mt-2 text-sm font-medium">
                Cuando el evento tenga gastos cargados, acá vas a ver cuánto le corresponde a cada
                integrante.
              </p>
            </div>
          </div>
        ) : (
          <section className="space-y-3">
            <h3 className="text-text-primary text-base font-extrabold">Saldo por integrante</h3>
            <ul className="grid gap-3">
              {state.balances.map((balance) => (
                <BalanceCard
                  key={balance.memberId}
                  balance={balance}
                  currency={event.baseCurrency}
                />
              ))}
            </ul>
          </section>
        ))}
    </div>
  );
}

function balanceTone(amount: number) {
  if (amount > 0) return "recibe" as const;
  if (amount < 0) return "debe" as const;
  return "en cero" as const;
}

function balanceAmount(amount: number, currency: string) {
  const formatted = formatMoney(amount, currency);
  return amount > 0 ? `+${formatted}` : formatted;
}

function BalanceCard({ balance, currency }: { balance: MemberBalance; currency: string }) {
  const tone = balanceTone(balance.netBalance);

  return (
    <li>
      <article className="border-border flex items-center gap-3 rounded-[24px] border p-4">
        <Avatar className="size-10">
          <AvatarFallback>{getInitials(balance.memberName)}</AvatarFallback>
        </Avatar>
        <div className="min-w-0 flex-1">
          <p className="text-text-primary truncate text-sm font-extrabold">{balance.memberName}</p>
          <span
            className={`mt-1 inline-flex rounded-full px-2 py-0.5 text-[11px] font-black ${
              tone === "recibe"
                ? "bg-primary/10 text-primary"
                : tone === "debe"
                  ? "bg-destructive/10 text-destructive"
                  : "bg-muted text-text-secondary"
            }`}
          >
            {tone}
          </span>
        </div>
        <p
          className={`shrink-0 text-sm font-extrabold ${
            tone === "recibe"
              ? "text-primary"
              : tone === "debe"
                ? "text-destructive"
                : "text-text-primary"
          }`}
        >
          {balanceAmount(balance.netBalance, currency)}
        </p>
      </article>
    </li>
  );
}
