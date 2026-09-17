"use client";

import { useState } from "react";
import { Trash2 } from "lucide-react";

import { deleteEvent, EventError, type EventDetail } from "@/lib/events";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/components/ui/dialog";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/ui/spinner";

type DeleteEventDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  event: EventDetail;
  onDeleted: () => void;
  onUnauthorized: () => void;
};

export function DeleteEventDialog({
  open,
  onOpenChange,
  event,
  onDeleted,
  onUnauthorized,
}: DeleteEventDialogProps) {
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
      await deleteEvent(event.id);
      onDeleted();
    } catch (err) {
      if (err instanceof EventError && err.type === "unauthorized") {
        onUnauthorized();
        return;
      }
      setError(
        err instanceof EventError ? err.message : "No pudimos eliminar el evento. Probá de nuevo."
      );
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        aria-label="Eliminar evento"
        className="border-border shadow-none sm:max-w-[480px]"
      >
        <div>
          <span
            aria-hidden="true"
            className="bg-destructive/10 text-destructive mb-2 flex size-12 items-center justify-center rounded-[16px]"
          >
            <Trash2 className="size-6" />
          </span>
          <DialogTitle className="text-[28px]">Eliminar «{event.name}»</DialogTitle>
          <DialogDescription className="text-muted-foreground text-sm leading-6 font-medium">
            Se eliminan también{" "}
            {event.memberCount === 1 ? "1 integrante" : `${event.memberCount} integrantes`}. El
            resto de los integrantes deja de ver el evento. Esta acción no se puede deshacer.
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
            variant="ghost"
            disabled={isDeleting}
            onClick={() => onOpenChange(false)}
            className="text-muted-foreground hover:text-foreground mt-0 h-10 rounded-[8px] border-0 px-4 text-xl font-medium"
          >
            Cancelar
          </Button>
          <Button
            type="button"
            disabled={isDeleting}
            onClick={handleConfirm}
            className="bg-destructive hover:bg-destructive/90 h-10 gap-2 rounded-[8px] px-4 text-xl font-medium text-white"
          >
            {isDeleting ? <Spinner className="size-4" /> : "Eliminar evento"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
