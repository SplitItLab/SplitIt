"use client";

import { useState } from "react";
import { Trash2 } from "lucide-react";

import { EventError } from "@/lib/events";
import { deleteExpense, type Expense } from "@/lib/expenses";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { LoadingIndicator } from "@/components/loading-indicator";

type DeleteExpenseDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  expense: Expense;
  onDeleted: (expense: Expense) => void;
  onAlreadyDeleted: (expense: Expense) => void;
  onUnauthorized: () => void;
  onNotFound: () => void;
};

export function DeleteExpenseDialog({
  open,
  onOpenChange,
  expense,
  onDeleted,
  onAlreadyDeleted,
  onUnauthorized,
  onNotFound,
}: DeleteExpenseDialogProps) {
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleOpenChange = (next: boolean) => {
    if (next) setError(null);
    onOpenChange(next);
  };

  const handleConfirm = async () => {
    if (isDeleting) return;
    setIsDeleting(true);
    setError(null);
    try {
      await deleteExpense(expense.eventId, expense.id);
      onDeleted(expense);
    } catch (err) {
      if (err instanceof EventError && err.type === "unauthorized") {
        onUnauthorized();
        return;
      }
      if (err instanceof EventError && err.type === "not-found") {
        onAlreadyDeleted(expense);
        return;
      }
      if (err instanceof EventError && err.type === "forbidden") {
        onNotFound();
        return;
      }
      setError(
        err instanceof EventError ? err.message : "No pudimos eliminar el gasto. Probá de nuevo."
      );
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        aria-label="Eliminar gasto"
        className="border-border h-[255px] px-8 py-6 shadow-none sm:max-w-[512px]"
      >
        <div>
          <span
            aria-hidden="true"
            className="bg-destructive/10 text-destructive mb-2 flex size-12 items-center justify-center rounded-[16px]"
          >
            <Trash2 className="size-6" />
          </span>
          <DialogTitle className="text-[18px]">
            ¿Estás seguro que querés eliminar este gasto?
          </DialogTitle>
          <DialogDescription className="text-muted-foreground text-sm">
            Vas a eliminar {expense.name}. Esta acción también actualiza los saldos del evento.
          </DialogDescription>
        </div>

        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="outline"
            disabled={isDeleting}
            onClick={() => onOpenChange(false)}
            className="text-foreground h-9 rounded-[18px] px-4 py-2 text-sm font-medium shadow-xs"
          >
            Cancelar
          </Button>
          <Button
            type="button"
            disabled={isDeleting}
            onClick={handleConfirm}
            className="bg-destructive hover:bg-destructive/90 h-9 gap-2 rounded-[18px] px-4 py-2 text-sm font-medium text-white"
          >
            {isDeleting ? <LoadingIndicator message="Eliminando gasto…" /> : "Eliminar"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
