"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Info, UserRound, Users } from "lucide-react";

import type { GuestSession } from "@/lib/auth";
import { EventError } from "@/lib/events";
import {
  enterAsGuest,
  listInvitationMembers,
  recalledGuestMemberId,
  type InvitationMember,
} from "@/lib/invitations";
import { showAppToast } from "@/lib/toast";
import { InvitationMemberSelect } from "@/components/invitation-member-select";
import { LoadingIndicator } from "@/components/loading-indicator";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button, buttonVariants } from "@/components/ui/button";

type LoadState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "ready"; members: InvitationMember[] };

const ACTION_CLASS_NAME = "h-10 w-full gap-2 rounded-[8px] px-4 text-xl font-medium";

function eventPath(eventId: number): string {
  return `/eventos/${eventId}`;
}

function recalledSelection(token: string, members: InvitationMember[]): string {
  const recalled = recalledGuestMemberId(token);
  const member = members.find((candidate) => candidate.id === recalled);
  return member && !member.isLinked ? String(member.id) : "";
}

export function GuestMemberForm({
  token,
  guest,
  onGuestNotInEvent,
}: {
  token: string;
  guest: GuestSession | null;
  onGuestNotInEvent?: () => void;
}) {
  const router = useRouter();
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [selectedMemberId, setSelectedMemberId] = useState("");
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [changing, setChanging] = useState(false);

  const fetchMembers = useCallback(() => {
    listInvitationMembers(token)
      .then((members) => {
        setSelectedMemberId(recalledSelection(token, members));
        setState({ status: "ready", members });
      })
      .catch((err) => {
        setState({
          status: "error",
          message:
            err instanceof EventError
              ? err.message
              : "No pudimos cargar los integrantes. Probá de nuevo.",
        });
      });
  }, [token]);

  useEffect(() => {
    fetchMembers();
  }, [fetchMembers]);

  const retry = () => {
    setState({ status: "loading" });
    fetchMembers();
  };

  // La cookie de invitado puede ser de otro evento: solo vale si su integrante está en este link.
  const guestInEvent =
    state.status === "ready" && guest
      ? state.members.some((member) => member.id === guest.memberId)
      : null;

  useEffect(() => {
    if (guestInEvent === false) onGuestNotInEvent?.();
  }, [guestInEvent, onGuestNotInEvent]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedMemberId) {
      setSubmitError("Elegí quién sos de la lista.");
      return;
    }

    setSubmitError(null);
    setSubmitting(true);
    try {
      const entered = await enterAsGuest(token, Number(selectedMemberId));
      showAppToast("success", `Entraste al evento como ${entered.name}`);
      router.push(eventPath(entered.eventId));
    } catch (err) {
      setSubmitting(false);
      setSubmitError(
        err instanceof EventError ? err.message : "No pudimos hacerte entrar. Probá de nuevo."
      );
      if (err instanceof EventError && err.type === "conflict") {
        setSelectedMemberId("");
        fetchMembers();
      }
    }
  };

  if (state.status === "loading") {
    return (
      <div className="flex justify-center py-6">
        <LoadingIndicator message="Buscando a los integrantes…" />
      </div>
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

  if (guest && guestInEvent && !changing) {
    return (
      <div className="flex flex-col gap-[10px]">
        <div className="bg-soft-info flex gap-2 rounded-[16px] p-4">
          <Info aria-hidden="true" className="text-info mt-1 size-4 shrink-0" />
          <p className="text-muted-foreground text-sm leading-6 font-medium">
            Ya elegiste ser <span className="text-foreground font-extrabold">{guest.name}</span> en
            este evento.
          </p>
        </div>
        <Link
          href={eventPath(guest.eventId)}
          className={buttonVariants({ className: ACTION_CLASS_NAME })}
        >
          <Users aria-hidden="true" />
          Entrar al evento
        </Link>
        <Button
          type="button"
          variant="outline"
          className={ACTION_CLASS_NAME}
          onClick={() => setChanging(true)}
        >
          No soy yo
        </Button>
      </div>
    );
  }

  const noMemberAvailable = state.members.every((member) => member.isLinked);
  const recalledLinkedMember = state.members.find(
    (member) => member.id === recalledGuestMemberId(token) && member.isLinked
  );

  return (
    <form className="flex flex-col gap-[10px]" onSubmit={handleSubmit} noValidate>
      {recalledLinkedMember && (
        <div className="bg-soft-info flex gap-2 rounded-[16px] p-4">
          <Info aria-hidden="true" className="text-info mt-1 size-4 shrink-0" />
          <p className="text-muted-foreground text-sm leading-6 font-medium">
            <span className="text-foreground font-extrabold">{recalledLinkedMember.name}</span> ya
            está vinculado a una cuenta, así que no podés seguir entrando como esa persona.{" "}
            {!noMemberAvailable && "Elegí otro integrante disponible."}
          </p>
        </div>
      )}
      {noMemberAvailable && (
        <div className="bg-soft-info flex gap-2 rounded-[16px] p-4">
          <Info aria-hidden="true" className="text-info mt-1 size-4 shrink-0" />
          <p className="text-muted-foreground text-sm leading-6 font-medium">
            Todos los integrantes ya tienen una cuenta. Entrá con la tuya.
          </p>
        </div>
      )}

      <div className="flex flex-col gap-[6px]">
        <label htmlFor="guest-member" className="text-foreground text-sm font-medium">
          ¿Quién sos en este evento?
        </label>
        <InvitationMemberSelect
          id="guest-member"
          members={state.members}
          value={selectedMemberId}
          onChange={(value) => {
            setSelectedMemberId(value);
            setSubmitError(null);
          }}
          invalid={!!submitError}
        />
        <p className="text-muted-foreground text-sm font-medium">
          Entrás sin cuenta, solo a este evento. Podés salir cuando quieras.
        </p>
      </div>

      {submitError && (
        <Alert variant="destructive">
          <AlertDescription>{submitError}</AlertDescription>
        </Alert>
      )}

      <Button
        type="submit"
        disabled={submitting || noMemberAvailable}
        className={ACTION_CLASS_NAME}
      >
        {submitting ? (
          <LoadingIndicator message="Entrando al evento…" />
        ) : (
          <>
            <UserRound aria-hidden="true" />
            Entrar como invitado
          </>
        )}
      </Button>
    </form>
  );
}
