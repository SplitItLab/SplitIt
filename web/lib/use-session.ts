"use client";

import { useEffect, useState } from "react";
import { getSession, SessionUser } from "@/lib/auth";

export type SessionState =
  | { status: "loading" }
  | { status: "authenticated"; user: SessionUser }
  | { status: "unauthenticated" };

export function useSession(): SessionState {
  const [state, setState] = useState<SessionState>({ status: "loading" });

  useEffect(() => {
    let cancelled = false;

    getSession().then((user) => {
      if (cancelled) return;
      setState(user ? { status: "authenticated", user } : { status: "unauthenticated" });
    });

    return () => {
      cancelled = true;
    };
  }, []);

  return state;
}
