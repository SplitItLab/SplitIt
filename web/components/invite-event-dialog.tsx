"use client";

import { useEffect, useState } from "react";
import { UserPlus, X } from "lucide-react";

import {
  buildEventInviteUrl,
  EventError,
  getEventInviteToken,
  type EventDetail,
} from "@/lib/events";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Input } from "@/components/ui/input";
import { Spinner } from "@/components/ui/spinner";

type InviteEventDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  event: EventDetail;
  onUnauthorized: () => void;
};

type LoadState =
  { status: "loading" } | { status: "ready"; token: string } | { status: "error"; message: string };

export function InviteEventDialog({
  open,
  onOpenChange,
  event,
  onUnauthorized,
}: InviteEventDialogProps) {
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [copied, setCopied] = useState(false);
  const [copyError, setCopyError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);

  const resetLocalState = () => {
    setCopied(false);
    setCopyError(null);
    setState({ status: "loading" });
  };

  const handleOpenChange = (next: boolean) => {
    if (!next) resetLocalState();
    onOpenChange(next);
  };

  useEffect(() => {
    if (!open) return;

    let cancelled = false;

    getEventInviteToken(event.id)
      .then((token) => {
        if (!cancelled) setState({ status: "ready", token });
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof EventError && err.type === "unauthorized") {
          onUnauthorized();
          return;
        }
        setState({
          status: "error",
          message:
            err instanceof EventError
              ? err.message
              : "No pudimos generar el enlace. Probá de nuevo.",
        });
      });

    return () => {
      cancelled = true;
    };
  }, [open, event.id, retryKey, onUnauthorized]);

  const inviteUrl = state.status === "ready" ? buildEventInviteUrl(state.token) : "";

  const handleCopy = async () => {
    if (state.status !== "ready") return;
    try {
      await navigator.clipboard.writeText(inviteUrl);
      setCopied(true);
      setCopyError(null);
    } catch {
      setCopied(false);
      setCopyError("No pudimos copiar el enlace. Probá de nuevo.");
    }
  };

  const retry = () => {
    resetLocalState();
    setRetryKey((key) => key + 1);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent aria-label="Invitar al evento" className="sm:max-w-[480px]">
        <div className="flex items-start justify-between gap-3">
          <div>
            <span
              aria-hidden="true"
              className="bg-primary/10 text-primary mb-2 flex size-12 items-center justify-center rounded-[16px]"
            >
              <UserPlus className="size-6" />
            </span>
            <DialogTitle className="text-[28px]">Invitar al evento</DialogTitle>
            <DialogDescription className="text-muted-foreground mt-1 text-sm leading-6 font-medium">
              Este enlace no vence ni puede revocarse. Cualquiera con el enlace puede usarlo.
            </DialogDescription>
          </div>
          <DialogClose
            aria-label="Cerrar"
            className="text-text-secondary hover:text-text-primary hover:bg-muted -mt-1 -mr-2 flex size-10 shrink-0 items-center justify-center rounded-[8px] transition-colors"
          >
            <X className="size-5" />
          </DialogClose>
        </div>

        {state.status === "loading" && (
          <section className="flex items-center justify-center gap-2 py-6">
            <Spinner className="size-5" />
            <p className="text-text-secondary text-sm font-medium">Generando enlace…</p>
          </section>
        )}

        {state.status === "ready" && (
          <div className="flex flex-col gap-3">
            <Input
              readOnly
              value={inviteUrl}
              aria-label="Enlace de invitación"
              className="h-auto rounded-[6px] px-3 py-2 text-base"
            />
            <Button
              type="button"
              onClick={handleCopy}
              className="h-10 rounded-[8px] px-4 text-xl font-medium"
            >
              {copied ? "Enlace copiado" : "Copiar enlace"}
            </Button>
            {copied && (
              <p role="status" className="text-sm font-medium">
                Enlace copiado
              </p>
            )}
            {copyError && (
              <Alert variant="destructive">
                <AlertDescription>{copyError}</AlertDescription>
              </Alert>
            )}
          </div>
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
      </DialogContent>
    </Dialog>
  );
}
