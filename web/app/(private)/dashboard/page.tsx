"use client";

import { useSession } from "@/lib/use-session";

export default function DashboardPage() {
  const session = useSession();
  const name = session.status === "authenticated" ? session.user.name : "";

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-2">
      <h1 className="text-2xl font-semibold">Dashboard</h1>
      {name && <p className="text-muted-foreground">Bienvenida, {name}.</p>}
    </div>
  );
}
