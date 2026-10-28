"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Info, UserRoundCheck, Users } from "lucide-react";

import type { SessionUser } from "@/lib/auth";
import { EventError } from "@/lib/events";
import {
  getInvitationAccess,
  invitationLoginPath,
  linkInvitationMember,
  listInvitationMembers,
  type InvitationAccess,
  type InvitationMember,
} from "@/lib/invitations";
import { showAppToast } from "@/lib/toast";
import { LoadingIndicator } from "@/components/loading-indicator";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button, buttonVariants } from "@/components/ui/button";

type LoadState =
  | { status: "loading" }
  | { status: "error"; message: string }
  | { status: "linked"; eventId: number; memberName: string }
  | { status: "choosing"; eventId: number; members: InvitationMember[] };

const ACTION_CLASS_NAME = "h-10 w-full gap-2 rounded-[8px] px-4 text-xl font-medium";

function eventPath(eventId: number): string {
  return `/eventos/${eventId}`;
}

function toLoadState(access: InvitationAccess, members: InvitationMember[]): LoadState {
  if (access.linkedMember) {
    return { status: "linked", eventId: access.eventId, memberName: access.linkedMember.name };
  }
  return { status: "choosing", eventId: access.eventId, members };
}

export function LinkMemberForm({ token, user }: { token: string; user: SessionUser }) {
  const router = useRouter();
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [selectedMemberId, setSelectedMemberId] = useState("");
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleError = useCallback(
    (err: unknown, fallbackMessage: string): string | null => {
      if (err instanceof EventError && err.type === "unauthorized") {
        router.replace(invitationLoginPath(token));
        return null;
      }
      return err instanceof EventError ? err.message : fallbackMessage;
    },
    [router, token]
  );

  const fetchAccess = useCallback(() => {
    Promise.all([getInvitationAccess(token), listInvitationMembers(token)])
      .then(([access, members]) => setState(toLoadState(access, members)))
      .catch((err) => {
        const message = handleError(err, "No pudimos cargar los integrantes. Probá de nuevo.");
        if (message) setState({ status: "error", message });
      });
  }, [token, handleError]);

  useEffect(() => {
    fetchAccess();
  }, [fetchAccess]);

  const retry = () => {
    setState({ status: "loading" });
    fetchAccess();
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (state.status !== "choosing") return;
    if (!selectedMemberId) {
      setSubmitError("Elegí quién sos de la lista.");
      return;
    }

    setSubmitError(null);
    setSubmitting(true);
    try {
      const access = await linkInvitationMember(token, Number(selectedMemberId));
      showAppToast(
        "success",
        `Entraste al evento como ${access.linkedMember?.name ?? "integrante"}`
      );
      router.push(eventPath(access.eventId));
    } catch (err) {
      setSubmitting(false);
      const message = handleError(err, "No pudimos vincular tu cuenta. Probá de nuevo.");
      if (message) setSubmitError(message);
      if (err instanceof EventError && err.type === "conflict") {
        setSelectedMemberId("");
        fetchAccess();
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

  if (state.status === "linked") {
    return (
      <div className="flex flex-col gap-[10px]">
        <div className="bg-soft-info flex gap-2 rounded-[16px] p-4">
          <Info aria-hidden="true" className="text-info mt-1 size-4 shrink-0" />
          <p className="text-muted-foreground text-sm leading-6 font-medium">
            Ya estás en este evento como{" "}
            <span className="text-foreground font-extrabold">{state.memberName}</span>, con la
            cuenta de {user.email}.
          </p>
        </div>
        <Link
          href={eventPath(state.eventId)}
          className={buttonVariants({ className: ACTION_CLASS_NAME })}
        >
          <Users aria-hidden="true" />
          Entrar al evento
        </Link>
      </div>
    );
  }

  const noMemberAvailable = state.members.every((member) => member.isLinked);

  return (
    <form className="flex flex-col gap-[10px]" onSubmit={handleSubmit} noValidate>
      {noMemberAvailable && (
        <div className="bg-soft-info flex gap-2 rounded-[16px] p-4">
          <Info aria-hidden="true" className="text-info mt-1 size-4 shrink-0" />
          <p className="text-muted-foreground text-sm leading-6 font-medium">
            Todos los integrantes ya tienen una cuenta vinculada
          </p>
        </div>
      )}

      <div className="flex flex-col gap-[6px]">
        <label htmlFor="link-member" className="text-foreground text-sm font-medium">
          ¿Quién sos en este evento?
        </label>
        <select
          id="link-member"
          value={selectedMemberId}
          onChange={(event) => {
            setSelectedMemberId(event.target.value);
            setSubmitError(null);
          }}
          aria-invalid={!!submitError}
          className="border-border text-foreground focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:border-destructive h-auto w-full rounded-[6px] border bg-white px-3 py-2 text-base leading-6 outline-none focus-visible:ring-3"
        >
          <option value="" disabled>
            Elegite de la lista
          </option>
          {state.members.map((member) => (
            <option key={member.id} value={member.id} disabled={member.isLinked}>
              {member.isLinked ? `${member.name} · ya entró con su cuenta` : member.name}
            </option>
          ))}
        </select>
        <p className="text-muted-foreground text-sm font-medium">
          Entrás con la cuenta de {user.email}. El evento te va a quedar entre tus eventos.
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
          <LoadingIndicator message="Vinculando tu cuenta…" />
        ) : (
          <>
            <UserRoundCheck aria-hidden="true" />
            Soy esta persona
          </>
        )}
      </Button>
    </form>
  );
}
