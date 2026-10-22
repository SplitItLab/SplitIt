"use client";

import { useCallback, useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { UserRoundCheck } from "lucide-react";

import { EventError } from "@/lib/events";
import { getInvitation, invitationLoginPath, type Invitation } from "@/lib/invitations";
import { useSession } from "@/lib/use-session";
import { EventIcon } from "@/components/event-icon";
import { EventNotFound } from "@/components/event-not-found";
import { LinkMemberForm } from "@/components/link-member-form";
import { LoadingIndicator } from "@/components/loading-indicator";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button, buttonVariants } from "@/components/ui/button";

type LoadState =
  | { status: "loading" }
  | { status: "not-found" }
  | { status: "error"; message: string }
  | { status: "loaded"; invitation: Invitation };

export function EventInvitationView({ token }: { token: string }) {
  const session = useSession();
  const [state, setState] = useState<LoadState>({ status: "loading" });

  const fetchInvitation = useCallback(() => {
    getInvitation(token)
      .then((invitation) => setState({ status: "loaded", invitation }))
      .catch((err) => {
        if (err instanceof EventError && err.type === "not-found") {
          setState({ status: "not-found" });
          return;
        }
        setState({
          status: "error",
          message:
            err instanceof EventError
              ? err.message
              : "No pudimos cargar la invitación. Probá de nuevo.",
        });
      });
  }, [token]);

  useEffect(() => {
    fetchInvitation();
  }, [fetchInvitation]);

  const retry = () => {
    setState({ status: "loading" });
    fetchInvitation();
  };

  const isLoading =
    state.status === "loading" || (state.status === "loaded" && session.status === "loading");

  return (
    <div className="relative grid min-h-screen md:grid-cols-2">
      <div className="relative hidden min-h-screen overflow-hidden md:block">
        <Image src="/register-image.png" alt="" fill unoptimized className="object-cover" />
      </div>

      <div className="flex flex-col items-center justify-center px-6 py-10">
        <div className="w-full max-w-sm space-y-[10px]">
          {isLoading && (
            <div className="flex justify-center py-6">
              <LoadingIndicator message="Cargando la invitación…" />
            </div>
          )}

          {state.status === "not-found" && <EventNotFound />}

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

          {state.status === "loaded" && session.status !== "loading" && (
            <>
              <InvitationHeader invitation={state.invitation} />
              {session.status === "authenticated" ? (
                <LinkMemberForm token={token} user={session.user} />
              ) : (
                <div className="flex flex-col gap-[10px]">
                  <Link
                    href={invitationLoginPath(token)}
                    className={buttonVariants({
                      className: "h-10 w-full gap-2 rounded-[8px] px-4 text-xl font-medium",
                    })}
                  >
                    <UserRoundCheck aria-hidden="true" />
                    Continuar con mi cuenta
                  </Link>
                  <p className="text-muted-foreground text-center text-sm">
                    Con cuenta, el evento «{state.invitation.name}» te queda guardado entre tus
                    eventos.
                  </p>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function InvitationHeader({ invitation }: { invitation: Invitation }) {
  return (
    <div className="flex flex-col items-center gap-3">
      <EventIcon iconKey={invitation.iconKey} className="size-16 rounded-[18px]" />
      <div className="flex flex-col items-center justify-center gap-2 text-center">
        <h1 className="text-[32px] font-extrabold text-black">{invitation.name}</h1>
        {invitation.description && (
          <p className="text-muted-foreground text-sm font-medium">{invitation.description}</p>
        )}
      </div>
      <p className="text-muted-foreground text-sm font-medium">
        <span className="text-foreground font-extrabold">{invitation.memberCount}</span>{" "}
        {invitation.memberCount === 1 ? "participante" : "participantes"}
      </p>
    </div>
  );
}
