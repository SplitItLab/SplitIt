"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Pencil, Plus, Receipt } from "lucide-react";

import { EventError, type EventDetail } from "@/lib/events";
import {
  expenseTotal,
  formatExpenseDate,
  formatMoney,
  listExpenses,
  type Expense,
} from "@/lib/expenses";
import { showAppToast } from "@/lib/toast";
import { AddExpenseDialog } from "@/components/add-expense-dialog";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { LoadingIndicator } from "@/components/loading-indicator";

type LoadState =
  | { status: "loading" }
  | { status: "ready"; expenses: Expense[] }
  | { status: "error"; message: string };

export function EventExpenses({
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
    listExpenses(event.id)
      .then((expenses) => {
        if (current !== requestId.current) return;
        setState({ status: "ready", expenses });
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
              : "No pudimos cargar los gastos. Probá de nuevo.",
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
      <h2 className="text-text-primary text-2xl font-extrabold">Gastos</h2>

      {state.status === "loading" && (
        <section className="flex items-center justify-center gap-2 py-16">
          <LoadingIndicator
            message="Cargando los gastos…"
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

      {state.status === "ready" && (
        <ExpenseList
          event={event}
          expenses={state.expenses}
          onCreated={(expense) => {
            setState((current) =>
              current.status === "ready"
                ? { status: "ready", expenses: [expense, ...current.expenses] }
                : current
            );
            showAppToast("success", `Agregamos «${expense.name}»`);
          }}
          onUpdate={(updated) => {
            setState((current) =>
              current.status === "ready"
                ? {
                    status: "ready",
                    expenses: current.expenses.map((expense) =>
                      expense.id === updated.id ? updated : expense
                    ),
                  }
                : current
            );
            showAppToast("success", `Guardamos «${updated.name}»`);
          }}
          onUnauthorized={onUnauthorized}
          onNotFound={onNotFound}
        />
      )}
    </div>
  );
}

function ExpenseList({
  event,
  expenses,
  onCreated,
  onUnauthorized,
  onNotFound,
  onUpdate,
}: {
  event: EventDetail;
  expenses: Expense[];
  onCreated: (expense: Expense) => void;
  onUnauthorized: () => void;
  onNotFound: () => void;
  onUpdate: (expense: Expense) => void;
}) {
  const [adding, setAdding] = useState(false);
  const [editingExpense, setEditingExpense] = useState<Expense | null>(null);
  return (
    <>
      {expenses.length > 0 && (
        <article className="border-border rounded-[24px] border p-4">
          <p className="text-text-secondary text-xs font-bold">Total de gastos</p>
          <p className="text-text-primary mt-1 text-2xl font-extrabold">
            {formatMoney(expenseTotal(expenses), event.baseCurrency)}
          </p>
        </article>
      )}

      <div className="grid gap-3 md:grid-cols-[220px]">
        <Button
          type="button"
          className="h-14 w-full gap-2 rounded-[18px] text-base font-extrabold"
          onClick={() => setAdding(true)}
        >
          <Plus className="size-5" />
          Agregar gasto
        </Button>
      </div>
      <AddExpenseDialog
        open={adding}
        onOpenChange={setAdding}
        event={event}
        onCreated={onCreated}
        onUnauthorized={onUnauthorized}
        onNotFound={onNotFound}
      />
      {editingExpense && (
        <AddExpenseDialog
          key={editingExpense.id}
          open
          onOpenChange={(open) => {
            if (!open) setEditingExpense(null);
          }}
          event={event}
          expense={editingExpense}
          onCreated={onCreated}
          onUpdate={onUpdate}
          onUnauthorized={onUnauthorized}
          onNotFound={onNotFound}
        />
      )}
      {expenses.length > 0 ? (
        <div className="grid gap-3 xl:grid-cols-2">
          {expenses.map((expense) => (
            <ExpenseCard
              key={expense.id}
              expense={expense}
              baseCurrency={event.baseCurrency}
              onUpdate={() => setEditingExpense(expense)}
            />
          ))}
        </div>
      ) : (
        <div className="border-border rounded-[24px] border p-6 sm:p-8">
          <div className="mx-auto max-w-sm text-center">
            <h3 className="text-text-primary text-2xl font-extrabold">Todavía no hay gastos</h3>
            <p className="text-text-secondary mt-2 text-sm font-medium">
              Cuando cargues gastos, van a aparecer acá para revisar quién pagó y cuánto
              corresponde.
            </p>
          </div>
        </div>
      )}
    </>
  );
}

function ExpenseCard({
  expense,
  baseCurrency,
  onUpdate,
}: {
  expense: Expense;
  baseCurrency: string;
  onUpdate: () => void;
}) {
  const showOriginal = expense.originalCurrency !== baseCurrency;

  return (
    <article className="border-border w-full rounded-[24px] border p-4">
      <div className="flex items-center gap-3">
        <span
          aria-hidden="true"
          className="bg-primary/10 text-primary flex size-10 shrink-0 items-center justify-center rounded-[12px]"
        >
          <Receipt className="size-5" />
        </span>
        <div className="min-w-0 flex-1">
          <h3 className="text-text-primary truncate text-sm font-extrabold">{expense.name}</h3>
          <p className="text-text-secondary mt-1 text-xs">
            {formatExpenseDate(expense.expenseDate)}
          </p>
          <p className="text-text-secondary mt-2 text-xs font-semibold">
            Pagó {expense.paidByMember.name}
          </p>
        </div>
        <div className="flex shrink-0 flex-col items-end">
          <p className="text-primary text-sm font-extrabold">
            {formatMoney(expense.baseAmount, baseCurrency)}
          </p>
          {showOriginal && (
            <p className="text-text-secondary mt-0.5 text-[11px] font-semibold">
              {formatMoney(expense.originalAmount, expense.originalCurrency)}
            </p>
          )}
          <div className="mt-3 flex justify-end gap-1">
            <Button
              type="button"
              variant="ghost"
              size="icon"
              aria-label={`Editar gasto ${expense.name}`}
              onClick={onUpdate}
              className="text-muted-foreground hover:text-foreground size-8 rounded-full"
            >
              <Pencil className="size-4" />
            </Button>
          </div>
        </div>
      </div>
    </article>
  );
}
