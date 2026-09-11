"use client";

import { createElement, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Check, Pencil, Plus, X } from "lucide-react";

import {
  createEvent,
  createEventSchema,
  DEFAULT_CURRENCY,
  EventError,
  EVENT_CURRENCIES,
  type CreateEventInput,
  type CreateEventPayload,
  type EventSummary,
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

const DEFAULT_ICON = EVENT_ICON_OPTIONS[0].key;

type CreateEventDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated: (event: EventSummary) => void;
  onUnauthorized: () => void;
  /** Nombre del usuario autenticado: siempre integra el evento. */
  currentUserName?: string;
};

export function CreateEventDialog({
  open,
  onOpenChange,
  onCreated,
  onUnauthorized,
  currentUserName,
}: CreateEventDialogProps) {
  const [participants, setParticipants] = useState<string[]>([]);
  const [memberDraft, setMemberDraft] = useState("");
  const [memberError, setMemberError] = useState<string | null>(null);
  const [iconKey, setIconKey] = useState(DEFAULT_ICON);
  const [iconOpen, setIconOpen] = useState(false);
  const [generalError, setGeneralError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreateEventInput, unknown, CreateEventPayload>({
    resolver: zodResolver(createEventSchema),
    defaultValues: {
      name: "",
      description: "",
      baseCurrency: DEFAULT_CURRENCY,
      participantNames: [],
    },
  });

  const resetForm = () => {
    reset({
      name: "",
      description: "",
      baseCurrency: DEFAULT_CURRENCY,
      participantNames: [],
    });
    setParticipants([]);
    setMemberDraft("");
    setMemberError(null);
    setIconKey(DEFAULT_ICON);
    setGeneralError(null);
  };

  const handleOpenChange = (next: boolean) => {
    if (!next) resetForm();
    onOpenChange(next);
  };

  const addParticipant = () => {
    const name = memberDraft.trim();
    if (!name) {
      setMemberError("Escribí un nombre para agregarlo.");
      return;
    }
    if (
      currentUserName?.trim().toLowerCase() === name.toLowerCase() ||
      participants.some((participant) => participant.toLowerCase() === name.toLowerCase())
    ) {
      setMemberError("Ese integrante ya está en la lista.");
      return;
    }
    setParticipants((current) => [...current, name]);
    setMemberDraft("");
    setMemberError(null);
  };

  const removeParticipant = (name: string) => {
    setParticipants((current) => current.filter((participant) => participant !== name));
  };

  const onSubmit = async (data: CreateEventPayload) => {
    setGeneralError(null);
    try {
      const created = await createEvent({ ...data, iconKey, participantNames: participants });
      onCreated(created);
      resetForm();
      onOpenChange(false);
    } catch (err) {
      if (err instanceof EventError && err.type === "unauthorized") {
        onUnauthorized();
        return;
      }
      setGeneralError(
        err instanceof EventError ? err.message : "No pudimos crear el evento. Probá de nuevo."
      );
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent aria-label="Crear evento">
        <div className="flex items-start justify-between gap-3">
          <DialogTitle>Crear evento</DialogTitle>
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
            <Label htmlFor="event-name">Nombre del evento</Label>
            <Input
              id="event-name"
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
            <Label htmlFor="event-description">
              Descripción <span className="text-text-secondary font-medium">(opcional)</span>
            </Label>
            <Textarea
              id="event-description"
              placeholder="Datos importantes para la distinción del evento"
              className="min-h-24 rounded-[6px] px-3 py-2 text-base"
              {...register("description")}
            />
          </div>

          <div className="flex flex-col gap-[6px]">
            <Label htmlFor="event-currency">Moneda del evento</Label>
            <select
              id="event-currency"
              aria-invalid={Boolean(errors.baseCurrency)}
              className="border-input focus-visible:border-ring focus-visible:ring-ring/50 text-text-primary w-fit rounded-[6px] border bg-transparent px-3 py-2 text-base outline-none focus-visible:ring-3"
              {...register("baseCurrency")}
            >
              {EVENT_CURRENCIES.map(({ code, label }) => (
                <option key={code} value={code}>
                  {label}
                </option>
              ))}
            </select>
            {errors.baseCurrency && (
              <p role="alert" className="text-destructive text-sm">
                {errors.baseCurrency.message}
              </p>
            )}
          </div>

          <div className="flex flex-col gap-[6px]">
            <Label htmlFor="event-member">Integrantes</Label>
            <p className="text-text-secondary text-sm font-medium">
              Las personas que entren al evento lo harán con alguno de estos nombres
            </p>
            <div className="flex gap-2">
              <Input
                id="event-member"
                placeholder="Nombre"
                value={memberDraft}
                onChange={(event) => {
                  setMemberDraft(event.target.value);
                  setMemberError(null);
                }}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    event.preventDefault();
                    addParticipant();
                  }
                }}
                aria-invalid={Boolean(memberError)}
                className="h-auto rounded-[6px] px-3 py-2 text-base"
              />
              <Button type="button" onClick={addParticipant} className="h-auto rounded-[6px] px-4">
                Agregar
              </Button>
            </div>
            {memberError && (
              <p role="alert" className="text-destructive text-sm">
                {memberError}
              </p>
            )}
            <ul className="mt-1 flex flex-col gap-1">
              {currentUserName && (
                <li className="bg-muted flex items-center justify-between gap-2 rounded-[8px] py-1 pr-1 pl-3">
                  <span className="text-text-primary truncate text-sm font-medium">
                    {currentUserName}
                    <span className="text-text-secondary ml-2 text-sm font-medium">vos</span>
                  </span>
                  <span className="text-primary flex size-9 shrink-0 items-center justify-center">
                    <Check className="size-4" />
                  </span>
                </li>
              )}
              {participants.map((name) => (
                <li
                  key={name}
                  className="bg-muted flex items-center justify-between gap-2 rounded-[8px] py-1 pr-1 pl-3"
                >
                  <span className="text-text-primary truncate text-sm font-medium">{name}</span>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    aria-label={"Quitar a " + name}
                    onClick={() => removeParticipant(name)}
                  >
                    <X className="size-4" />
                  </Button>
                </li>
              ))}
            </ul>
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
              {isSubmitting ? <Spinner className="size-5" /> : <Plus className="size-5" />}
              Crear evento
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
