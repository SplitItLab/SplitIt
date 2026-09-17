"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Menu, LayoutGrid, User } from "lucide-react";
import { useSession } from "@/lib/use-session";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const NAV_ITEMS = [
  { href: "/eventos", label: "Eventos", Icon: LayoutGrid },
  { href: "/perfil", label: "Perfil", Icon: User },
] as const;

export function AppHeader() {
  const session = useSession();
  const pathname = usePathname();
  const user = session.status === "authenticated" ? session.user : null;

  const isActive = (href: string) => pathname === href || pathname.startsWith(`${href}/`);

  return (
    <header className="flex items-center justify-between px-4 py-3 sm:px-6">
      <Link
        href="/eventos"
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
            {NAV_ITEMS.map(({ href, label, Icon }) => {
              const active = isActive(href);
              return (
                <DropdownMenuItem
                  key={href}
                  render={
                    <Link
                      href={href}
                      aria-current={active ? "page" : undefined}
                      className={`flex items-center gap-2 ${
                        active ? "text-primary" : "text-text-primary"
                      }`}
                    >
                      <Icon className="size-4" />
                      {label}
                    </Link>
                  }
                />
              );
            })}
          </DropdownMenuContent>
        </DropdownMenu>
      )}
    </header>
  );
}
