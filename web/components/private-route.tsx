"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useSession } from "@/lib/use-session";
import { LoadingIndicator } from "@/components/loading-indicator";

export function PrivateRoute({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const session = useSession();

  // Un invitado solo puede estar en su evento.
  const guestEventPath = session.status === "guest" ? `/eventos/${session.guest.eventId}` : null;
  const guestOutsideEvent =
    guestEventPath !== null &&
    pathname !== guestEventPath &&
    !pathname.startsWith(`${guestEventPath}/`);

  useEffect(() => {
    if (session.status === "unauthenticated") {
      router.replace("/login");
    }
    if (guestOutsideEvent) {
      router.replace(guestEventPath);
    }
  }, [session.status, guestOutsideEvent, guestEventPath, router]);

  if (session.status === "loading") {
    return (
      <div className="flex h-screen items-center justify-center">
        <LoadingIndicator message="Verificando tu sesión…" />
      </div>
    );
  }

  if (session.status === "unauthenticated" || guestOutsideEvent) {
    return null;
  }

  return <>{children}</>;
}
