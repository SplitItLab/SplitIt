"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "@/lib/use-session";
import { LoadingIndicator } from "@/components/loading-indicator";

export function PrivateRoute({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const session = useSession();

  useEffect(() => {
    if (session.status === "unauthenticated") {
      router.replace("/login");
    }
  }, [session.status, router]);

  if (session.status === "loading") {
    return (
      <div className="flex h-screen items-center justify-center">
        <LoadingIndicator message="Verificando tu sesión…" />
      </div>
    );
  }

  if (session.status === "unauthenticated") {
    return null;
  }

  return <>{children}</>;
}
