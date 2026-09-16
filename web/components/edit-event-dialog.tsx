"use client";

import { createElement, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Check, Pencil, X } from "lucide-react";

import {
  currencyLabel,
  EventError,
  updateEvent,
  updateEventSchema,
  type EventDetail,
  type EventSummary,
  type UpdateEventInput,
  type UpdateEventPayload,
} from "@/lib/events";
import { EVENT_ICON_OPTIONS, eventIconFor } from "@/components/event-icon";
import { Button } from "@/components/ui/button";
import { Dialog, DialogClose, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/ui/spinner";

type EditEventDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  event: EventDetail;
  onUpdated: (event: EventSummary) => void;
  onUnauthorized: () => void;
};

export function EditEventDialog({
  open,
  onOpenChange,
  event,
  onUpdated,
  onUnauthorized,
}: EditEventDialogProps) {
  const [iconKey, setIconKey] = useState(event.iconKey ?? EVENT_ICON_OPTIONS[0].key);
  const [iconOpen, setIconOpen] = useState(false);
  const [generalError, setGeneralError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<UpdateEventInput, unknown, UpdateEventPayload>({
    resolver: zodResolver(updateEventSchema),
    defaultValues: {
      name: event.name,
      description: event.description ?? "",
      iconKey: event.iconKey ?? "",
    },
  });
  const syncWithEvent = () => {
    reset({
      name: event.name,
      description: event.description ?? "",
      iconKey: event.iconKey ?? "",
    });
    setIconKey(event.iconKey ?? EVENT_ICON_OPTIONS[0].key);
    setGeneralError(null);
  };

  const handleOpenChange = (next: boolean) => {
    if (next) syncWithEvent();
    onOpenChange(next);
  };

  const onSubmit = async (data: UpdateEventPayload) => {
    setGeneralError(null);
    try {
      const updated = await updateEvent(event.id, { ...data, iconKey });
      onUpdated(updated);
      onOpenChange(false);
    } catch (err) {
      if (err instanceof EventError && err.type === "unauthorized") {
        onUnauthorized();
        return;
      }
      setGeneralError(
        err instanceof EventError ? err.message : "No pudimos guardar los cambios. Probá de nuevo."
      );
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent aria-label="Editar evento">
        <div className="flex items-start justify-between gap-3">
          <DialogTitle>Editar evento</DialogTitle>
          <DialogClose
            aria-label="Cerrar"
            className="text-text-secondary hover:text-text-primary hover:bg-muted -mt-1 -mr-2 flex size-10 shrink-0 items-center justify-center rounded-[8px] transition-colors"
          >
            <X className="size-5" />
          </DialogClose>
        </div>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
          <Popover open={iconOpen} onOpenChange={setIconOpen}>
            <PopoverTrigger
              aria-label="Cambiar icono del evento"
              className="focus-visible:ring-ring/50 relative w-fit rounded-[16px] outline-none focus-visible:ring-3"
            >
              <span className="bg-primary/10 text-primary flex size-14 items-center justify-center rounded-[16px]">
                {createElement(eventIconFor(iconKey), { className: "size-6" })}
              </span>
              <span className="border-card bg-primary text-primary-foreground absolute -right-1 -bottom-1 flex size-6 items-center justify-center rounded-full border-2">
                <Pencil className="size-3" />
              </span>
            </PopoverTrigger>
            <PopoverContent>
              <div className="grid grid-cols-4 gap-1">
                {EVENT_ICON_OPTIONS.map(({ key, label, Icon }) => (
                  <button
                    key={key}
                    type="button"
                    aria-label={label}
                    aria-pressed={key === iconKey}
                    onClick={() => {
                      setIconKey(key);
                      setIconOpen(false);
                    }}
                    className="text-text-primary hover:bg-muted focus-visible:ring-ring/50 aria-pressed:bg-primary/10 aria-pressed:text-primary flex size-12 items-center justify-center rounded-[12px] outline-none focus-visible:ring-3"
                  >
                    <Icon className="size-5" />
                  </button>
                ))}
              </div>
            </PopoverContent>
          </Popover>

          <div className="flex flex-col gap-[6px]">
            <Label htmlFor="edit-event-name">Nombre del evento</Label>
            <Input
              id="edit-event-name"
              placeholder="Viaje a la cordillera"
              aria-invalid={Boolean(errors.name)}
              className="h-auto rounded-[6px] px-3 py-2 text-base"
              {...register("name")}
            />
            {errors.name && (
              <p role="alert" className="text-destructive text-sm">
                {errors.name.message}
              </p>
            )}
          </div>

          <div className="flex flex-col gap-[6px]">
            <Label htmlFor="edit-event-description">
              Descripción <span className="text-text-secondary font-medium">(opcional)</span>
            </Label>
            <Textarea
              id="edit-event-description"
              placeholder="Datos importantes para la distinción del evento"
              className="min-h-24 rounded-[6px] px-3 py-2 text-base"
              {...register("description")}
            />
          </div>

          <div className="flex flex-col gap-[6px]">
            <Label>Moneda del evento</Label>
            <p
              aria-label="Moneda del evento"
              className="border-input text-text-primary bg-muted w-fit rounded-[6px] border px-3 py-2 text-base"
            >
              {currencyLabel(event.baseCurrency)}
            </p>
            <p className="text-text-secondary text-sm font-medium">
              La moneda no se puede cambiar después de crear el evento.
            </p>
          </div>

          {generalError && (
            <Alert variant="destructive">
              <AlertDescription>{generalError}</AlertDescription>
            </Alert>
          )}

          <div className="flex pt-2 sm:justify-end">
            <Button
              type="submit"
              disabled={isSubmitting}
              className="h-10 w-full rounded-[8px] px-5 text-xl font-medium sm:w-auto"
            >
              {isSubmitting ? <Spinner className="size-5" /> : <Check className="size-5" />}
              Guardar cambios
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
