"use client";

import { useEffect, useState } from "react";
import { getSession, GuestSession, SessionUser } from "@/lib/auth";

export type SessionState =
  | { status: "loading" }
  | { status: "authenticated"; user: SessionUser }
  | { status: "guest"; guest: GuestSession }
  | { status: "unauthenticated" };

export function useSession(): SessionState {
  const [state, setState] = useState<SessionState>({ status: "loading" });

  useEffect(() => {
    let cancelled = false;

    getSession()
      .then((session) => {
        if (cancelled) return;
        if (!session) return setState({ status: "unauthenticated" });
        setState(
          "type" in session
            ? { status: "guest", guest: session }
            : { status: "authenticated", user: session }
        );
      })
      .catch(() => {
        if (cancelled) return;
        setState({ status: "unauthenticated" });
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return state;
}
