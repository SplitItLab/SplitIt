"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Pencil, Plus, Receipt, Trash2 } from "lucide-react";

import { EventError, type EventDetail } from "@/lib/events";
import { formatExpenseDate, formatMoney, listExpenses, type Expense } from "@/lib/expenses";
import { showAppToast } from "@/lib/toast";
import { AddExpenseDialog } from "@/components/add-expense-dialog";
import { DeleteExpenseDialog } from "@/components/delete-expense-dialog";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { LoadingIndicator } from "@/components/loading-indicator";

type LoadState =
  | { status: "loading" }
  | {
      status: "ready";
      expenses: Expense[];
      hasNext: boolean;
      totalCount: number;
      totalAmount: number;
      loadingMore: boolean;
    }
  | { status: "error"; message: string };

type EventExpensesProps = {
  event: EventDetail;
  onUnauthorized: () => void;
  onNotFound: () => void;
};

export function useEventExpenses({ event, onUnauthorized, onNotFound }: EventExpensesProps) {
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const requestId = useRef(0);

  const load = useCallback(() => {
    const current = ++requestId.current;
    listExpenses(event.id)
      .then((page) => {
        if (current !== requestId.current) return;
        setState({
          status: "ready",
          expenses: page.items,
          hasNext: page.hasNext,
          totalCount: page.totalCount,
          totalAmount: page.totalAmount,
          loadingMore: false,
        });
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
    const requests = requestId;
    load();
    return () => {
      requests.current++;
    };
  }, [load]);

  const retry = () => {
    setState({ status: "loading" });
    load();
  };

  const loadMore = useCallback(() => {
    if (state.status !== "ready" || state.loadingMore || !state.hasNext) return;
    const current = requestId.current;
    setState({ ...state, loadingMore: true });
    listExpenses(event.id, state.expenses[state.expenses.length - 1])
      .then((page) => {
        if (current !== requestId.current) return;
        setState((latest) =>
          latest.status === "ready"
            ? {
                ...latest,
                expenses: [
                  ...latest.expenses,
                  // Un gasto cargado entre páginas no debe repetirse.
                  ...page.items.filter((item) => !latest.expenses.some((e) => e.id === item.id)),
                ],
                hasNext: page.hasNext,
                totalCount: page.totalCount,
                totalAmount: page.totalAmount,
                loadingMore: false,
              }
            : latest
        );
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
        setState((latest) =>
          latest.status === "ready" ? { ...latest, loadingMore: false } : latest
        );
        showAppToast(
          "error",
          err instanceof EventError ? err.message : "No pudimos cargar más gastos. Probá de nuevo."
        );
      });
  }, [event.id, onNotFound, onUnauthorized, state]);

  return { state, setState, load, retry, loadMore };
}

export function EventExpenses(props: EventExpensesProps) {
  const expenses = useEventExpenses(props);
  return <EventExpensesContent {...props} {...expenses} />;
}

export function EventExpensesContent({
  event,
  onUnauthorized,
  onNotFound,
  state,
  setState,
  load,
  retry,
  loadMore,
}: EventExpensesProps & ReturnType<typeof useEventExpenses>) {
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
          totalAmount={state.totalAmount}
          hasNext={state.hasNext}
          loadingMore={state.loadingMore}
          onLoadMore={loadMore}
          onCreated={(expense) => {
            setState((current) =>
              current.status === "ready"
                ? {
                    ...current,
                    expenses: [expense, ...current.expenses],
                    totalCount: current.totalCount + 1,
                    totalAmount: current.totalAmount + expense.baseAmount,
                  }
                : current
            );
            showAppToast("success", `Agregamos «${expense.name}»`);
          }}
          onUpdate={(updated) => {
            setState((current) =>
              current.status === "ready"
                ? {
                    ...current,
                    expenses: current.expenses.map((expense) =>
                      expense.id === updated.id ? updated : expense
                    ),
                    totalAmount:
                      current.totalAmount -
                      (current.expenses.find((expense) => expense.id === updated.id)?.baseAmount ??
                        updated.baseAmount) +
                      updated.baseAmount,
                  }
                : current
            );
            showAppToast("success", `Guardamos «${updated.name}»`);
          }}
          onDeleted={(deleted) => {
            setState((current) =>
              current.status === "ready"
                ? {
                    ...current,
                    expenses: current.expenses.filter((expense) => expense.id !== deleted.id),
                    totalCount: current.totalCount - 1,
                    totalAmount: current.totalAmount - deleted.baseAmount,
                  }
                : current
            );
            showAppToast("success", `Eliminamos «${deleted.name}»`);
          }}
          onAlreadyDeleted={(expense) => {
            // El 404 puede ser del gasto o del evento: recargar la lista lo distingue.
            load();
            showAppToast("info", `«${expense.name}» ya había sido eliminado`);
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
  totalAmount,
  hasNext,
  loadingMore,
  onLoadMore,
  onCreated,
  onUnauthorized,
  onNotFound,
  onUpdate,
  onDeleted,
  onAlreadyDeleted,
}: {
  event: EventDetail;
  expenses: Expense[];
  totalAmount: number;
  hasNext: boolean;
  loadingMore: boolean;
  onLoadMore: () => void;
  onCreated: (expense: Expense) => void;
  onUnauthorized: () => void;
  onNotFound: () => void;
  onUpdate: (expense: Expense) => void;
  onDeleted: (expense: Expense) => void;
  onAlreadyDeleted: (expense: Expense) => void;
}) {
  const [adding, setAdding] = useState(false);
  const [editingExpense, setEditingExpense] = useState<Expense | null>(null);
  const [deletingExpense, setDeletingExpense] = useState<Expense | null>(null);
  return (
    <>
      {expenses.length > 0 && (
        <article className="border-border rounded-[24px] border p-4">
          <p className="text-text-secondary text-xs font-bold">Total de gastos</p>
          <p className="text-text-primary mt-1 text-2xl font-extrabold">
            {formatMoney(totalAmount, event.baseCurrency)}
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
      {deletingExpense && (
        <DeleteExpenseDialog
          key={deletingExpense.id}
          open
          onOpenChange={(open) => {
            if (!open) setDeletingExpense(null);
          }}
          expense={deletingExpense}
          onDeleted={(deleted) => {
            setDeletingExpense(null);
            onDeleted(deleted);
          }}
          onAlreadyDeleted={(expense) => {
            setDeletingExpense(null);
            onAlreadyDeleted(expense);
          }}
          onUnauthorized={onUnauthorized}
          onNotFound={onNotFound}
        />
      )}
      {expenses.length > 0 ? (
        <>
          {/* grid-cols-1 (minmax(0,1fr)): sin columna explícita, un nombre largo ensancha la grilla. */}
          <div className="grid grid-cols-1 gap-3 xl:grid-cols-2">
            {expenses.map((expense) => (
              <ExpenseCard
                key={expense.id}
                expense={expense}
                baseCurrency={event.baseCurrency}
                // El invitado solo carga gastos propios: editar y borrar son del dueño y los integrantes con cuenta.
                readOnly={event.guestMemberId != null}
                onUpdate={() => setEditingExpense(expense)}
                onDelete={() => setDeletingExpense(expense)}
              />
            ))}
          </div>
          {hasNext && (
            <Button type="button" variant="outline" disabled={loadingMore} onClick={onLoadMore}>
              {loadingMore ? "Cargando…" : "Ver más gastos"}
            </Button>
          )}
        </>
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
  readOnly,
  onUpdate,
  onDelete,
}: {
  expense: Expense;
  baseCurrency: string;
  readOnly: boolean;
  onUpdate: () => void;
  onDelete: () => void;
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
          {!readOnly && (
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
              <Button
                type="button"
                variant="ghost"
                size="icon"
                aria-label={`Eliminar gasto ${expense.name}`}
                onClick={onDelete}
                className="text-muted-foreground hover:text-foreground size-8 rounded-full"
              >
                <Trash2 className="size-4" />
              </Button>
            </div>
          )}
        </div>
      </div>
    </article>
  );
}
