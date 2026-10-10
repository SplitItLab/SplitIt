"use client";

import { useEffect, useRef, useState } from "react";
import { ChevronDown } from "lucide-react";

import { EventError, type EventDetail } from "@/lib/events";
import {
  createExpense,
  EXPENSE_CURRENCIES,
  formatMoney,
  parseExpenseAmount,
  quoteExpense,
  type Expense,
  type ExpenseQuote,
  updateExpense,
} from "@/lib/expenses";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { LoadingIndicator } from "@/components/loading-indicator";

const QUOTE_ERROR = "No pudimos obtener el tipo de cambio.";
const selectClassName =
  "border-input focus-visible:border-ring focus-visible:ring-ring/50 text-text-primary w-full appearance-none rounded-[18px] border bg-transparent py-2 pr-9 pl-3 text-sm shadow-xs outline-none focus-visible:ring-3";
const selectChevronClassName =
  "text-muted-foreground pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2";

export function AddExpenseDialog({
  open,
  onOpenChange,
  event,
  onCreated,
  onUnauthorized,
  onNotFound,
  expense,
  onUpdate,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  event: EventDetail;
  onCreated: (expense: Expense) => void;
  onUnauthorized: () => void;
  onNotFound: () => void;
  expense?: Expense;
  onUpdate?: (expense: Expense) => void;
}) {
  const [name, setName] = useState(expense?.name ?? "");
  const [amount, setAmount] = useState(expense ? String(expense.originalAmount) : "");
  const [currency, setCurrency] = useState(expense?.originalCurrency ?? event.baseCurrency);
  const [payerId, setPayerId] = useState(
    expense ? String(expense.paidByMember.id) : memberId(event)
  );
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [quote, setQuote] = useState<ExpenseQuote | null>(null);
  const [quoteFailed, setQuoteFailed] = useState(false);
  const [quoteAttempt, setQuoteAttempt] = useState(0);
  const lastParsedAmountRef = useRef<number | null>(null);

  const parsedAmount = parseExpenseAmount(amount);
  const amountIsPositive = parsedAmount !== null;
  const needsQuote = currency !== event.baseCurrency;
  const quoteMatches =
    parsedAmount !== null &&
    quote !== null &&
    quote.originalCurrency === currency &&
    quote.originalAmount === parsedAmount;
  const waitingForQuote = needsQuote && amountIsPositive && !quoteMatches;

  const reset = () => {
    setName("");
    setAmount("");
    setCurrency(event.baseCurrency);
    setPayerId(memberId(event));
    setFormError(null);
    setSubmitting(false);
    setQuote(null);
    setQuoteFailed(false);
    setQuoteAttempt(0);
    lastParsedAmountRef.current = null;
  };

  const handleOpenChange = (next: boolean) => {
    if (!next) reset();
    onOpenChange(next);
  };

  const clearQuote = () => {
    setQuote(null);
    setQuoteFailed(false);
  };

  useEffect(() => {
    if (!open || !needsQuote || parsedAmount === null) return;

    const amountToQuote = parsedAmount;
    const controller = new AbortController();
    let cancelled = false;
    const timer = window.setTimeout(() => {
      quoteExpense(event.id, amountToQuote, currency, { signal: controller.signal })
        .then((result) => {
          if (cancelled) return;
          setQuote(result);
          setQuoteFailed(false);
        })
        .catch((err: unknown) => {
          if (cancelled || isAbort(err)) return;
          if (err instanceof EventError && err.type === "unauthorized") {
            onUnauthorized();
            return;
          }
          if (err instanceof EventError && (err.type === "forbidden" || err.type === "not-found")) {
            onNotFound();
            return;
          }
          setQuote(null);
          setQuoteFailed(true);
        });
    }, 300);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [
    currency,
    event.id,
    needsQuote,
    onNotFound,
    onUnauthorized,
    open,
    parsedAmount,
    quoteAttempt,
  ]);

  const submit = async () => {
    const trimmedName = name.trim();
    if (!trimmedName) {
      setFormError("Ingresá un nombre para el gasto");
      return;
    }
    if (parsedAmount === null) {
      setFormError("Ingresá un monto válido");
      return;
    }
    if (!payerId) {
      setFormError("Seleccioná quién pagó");
      return;
    }
    if (waitingForQuote || submitting) return;

    setFormError(null);
    setSubmitting(true);
    try {
      const input = {
        name: trimmedName,
        amount: parsedAmount,
        currency,
        paidByMemberId: Number(payerId),
      };
      if (expense) {
        const updated = await updateExpense(event.id, expense.id, input);
        onUpdate?.(updated);
      } else {
        const created = await createExpense(event.id, input);
        onCreated(created);
      }
      handleOpenChange(false);
    } catch (err: unknown) {
      if (err instanceof EventError && err.type === "unauthorized") {
        onUnauthorized();
        return;
      }
      if (err instanceof EventError && (err.type === "forbidden" || err.type === "not-found")) {
        onNotFound();
        return;
      }
      setFormError(
        err instanceof EventError ? err.message : "No pudimos guardar el gasto. Probá de nuevo."
      );
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent>
        <DialogTitle>{expense ? "Editar gasto" : "Agregar gasto"}</DialogTitle>
        <DialogDescription>
          {expense ? "Modificá el nombre, monto o quién pagó." : "Cargá quién pagó y el monto."}
        </DialogDescription>
        <form
          className="space-y-4"
          onSubmit={(event_) => {
            event_.preventDefault();
            void submit();
          }}
        >
          <div className="space-y-2">
            <Label htmlFor="expense-name">Nombre</Label>
            <Input
              id="expense-name"
              value={name}
              placeholder="Ej: Supermercado"
              className="rounded-[18px]"
              onChange={(event_) => {
                setName(event_.target.value);
                setFormError(null);
              }}
            />
          </div>

          <div className="grid grid-cols-[1fr_auto] gap-3">
            <div className="space-y-2">
              <Label htmlFor="expense-amount">Monto</Label>
              <Input
                id="expense-amount"
                inputMode="decimal"
                value={amount}
                placeholder="0"
                className="rounded-[18px]"
                onChange={(event_) => {
                  const next = event_.target.value;
                  const nextParsed = parseExpenseAmount(next);
                  if (nextParsed !== lastParsedAmountRef.current) {
                    lastParsedAmountRef.current = nextParsed;
                    clearQuote();
                  }
                  setAmount(next);
                  setFormError(null);
                }}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="expense-currency">Moneda</Label>
              <div className="relative w-[110px]">
                <select
                  id="expense-currency"
                  value={currency}
                  className={`${selectClassName} h-10`}
                  onChange={(event_) => {
                    setCurrency(event_.target.value);
                    setFormError(null);
                    clearQuote();
                  }}
                >
                  {EXPENSE_CURRENCIES.map((code) => (
                    <option key={code} value={code}>
                      {code}
                    </option>
                  ))}
                </select>
                <ChevronDown aria-hidden="true" className={selectChevronClassName} />
              </div>
            </div>
          </div>

          {needsQuote && (amountIsPositive || quoteFailed) && (
            <div className="border-border rounded-[16px] border px-3 py-2 text-xs font-semibold">
              {quoteFailed ? (
                <div className="flex flex-col items-start gap-2">
                  <p className="text-destructive" role="alert">
                    {QUOTE_ERROR}
                  </p>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setQuoteFailed(false);
                      setQuoteAttempt((attempt) => attempt + 1);
                    }}
                  >
                    Reintentar
                  </Button>
                </div>
              ) : quoteMatches && quote ? (
                <div className="text-text-secondary flex flex-col gap-0.5">
                  <span>
                    1 {currency} ={" "}
                    {quote.exchangeRate.toLocaleString("es-AR", { maximumFractionDigits: 4 })}{" "}
                    {event.baseCurrency}
                  </span>
                  <span className="text-text-primary">
                    Se guardará como {formatMoney(quote.baseAmount, event.baseCurrency)}
                  </span>
                </div>
              ) : (
                <span className="text-text-secondary">Obteniendo tipo de cambio…</span>
              )}
            </div>
          )}

          <div className="space-y-2">
            <Label htmlFor="expense-payer">Pago</Label>
            <div className="relative w-fit">
              <select
                id="expense-payer"
                value={payerId}
                className={`${selectClassName} h-11`}
                onChange={(event_) => {
                  setPayerId(event_.target.value);
                  setFormError(null);
                }}
              >
                {event.members.map((member) => (
                  <option key={member.id} value={member.id}>
                    {member.name}
                  </option>
                ))}
              </select>
              <ChevronDown aria-hidden="true" className={selectChevronClassName} />
            </div>
          </div>

          {formError && (
            <p className="text-destructive text-sm font-semibold" role="alert">
              {formError}
            </p>
          )}

          <div className="grid grid-cols-2 gap-3 pt-2">
            <Button
              type="button"
              variant="outline"
              className="rounded-[18px]"
              onClick={() => handleOpenChange(false)}
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              className="rounded-[18px]"
              disabled={submitting || waitingForQuote}
            >

              {submitting ?(<LoadingIndicator message= {expense? "Guardando cambios..." : "Guardando gasto…" } />
              ) : expense? (
                  "Guardar cambios" 
              ) : ( 
                 "Guardar gasto"
              )}    

            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function memberId(event: EventDetail) {
  return event.members[0] ? String(event.members[0].id) : "";
}

function isAbort(error: unknown) {
  return (
    typeof error === "object" &&
    error !== null &&
    "name" in error &&
    (error as { name?: unknown }).name === "AbortError"
  );
}
