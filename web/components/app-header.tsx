"use client";

import Link from "next/link";
import { Menu, LayoutGrid, User } from "lucide-react";
import { useSession } from "@/lib/use-session";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export function AppHeader() {
  const session = useSession();
  const user = session.status === "authenticated" ? session.user : null;

  return (
    <header className="flex items-center justify-between px-4 py-3 sm:px-6">
      <Link
        href="/dashboard"
        className="border-border flex items-center gap-0 rounded-full border px-3 py-2"
      >
        <span className="bg-primary text-primary-foreground flex h-[34px] w-[57px] items-center justify-center rounded-[8px] text-[24px] leading-[115%] font-extrabold">
          Split
        </span>
        <span className="text-[24px] leading-[115%] font-extrabold text-black">It</span>
      </Link>

      {user && (
        <DropdownMenu>
          <DropdownMenuTrigger
            className="bg-text-primary focus-visible:ring-ring/50 flex size-12 items-center justify-center gap-2.5 rounded-2xl text-white outline-none focus-visible:ring-3"
            aria-label="Menú de usuario"
          >
            <Menu className="size-5" />
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem
              render={
                <Link href="/dashboard" className="flex items-center gap-2">
                  <LayoutGrid className="size-4" />
                  Eventos
                </Link>
              }
            />
            <DropdownMenuItem
              render={
                <Link href="/perfil" className="flex items-center gap-2">
                  <User className="size-4" />
                  Perfil
                </Link>
              }
            />
          </DropdownMenuContent>
        </DropdownMenu>
      )}
    </header>
  );
}
