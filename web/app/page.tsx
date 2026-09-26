"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "@/lib/use-session";
import { Spinner } from "@/components/ui/spinner";
import { PrimaryLink } from "@/components/primary-link";
import { SplitItLogo } from "@/components/splitit-logo";
import { ArrowRight } from "lucide-react";

const PREVIEW_MEMBERS = [
  { initial: "N", name: "Nicolas", badge: "recibe", amount: "+$ 28.500", positive: true },
  { initial: "L", name: "Lucas", badge: "recibe", amount: "+$ 3.000", positive: true },
  { initial: "M", name: "Marcos", badge: "debe", amount: "-$ 12.500", positive: false },
  { initial: "F", name: "Francisco", badge: "debe", amount: "-$ 19.000", positive: false },
] as const;

function BalancePreviewCard() {
  return (
    <div className="w-full max-w-[380px] shrink-0 pb-16 lg:pb-0">
      <div className="border-border w-full max-w-[380px] shrink-0 rounded-[24px] border bg-white p-5 shadow-[0_18px_50px_rgba(7,27,58,0.12)]">
        <div className="mb-4">
          <p className="text-foreground text-[15px] font-black">Saldo por integrante</p>
          <p className="text-muted-foreground text-[12px]">Viaje a Bariloche</p>
        </div>
        <div className="flex flex-col gap-2">
          {PREVIEW_MEMBERS.map((m) => (
            <div key={m.initial} className="flex items-center gap-3">
              <span className="bg-muted text-foreground flex size-9 shrink-0 items-center justify-center rounded-full text-[12px] font-black">
                {m.initial}
              </span>
              <div className="min-w-0 flex-1">
                <p className="text-foreground truncate text-[13px] font-black">{m.name}</p>
                <span
                  className={`mt-0.5 inline-flex rounded-full px-2 py-0.5 text-[10px] font-black ${
                    m.positive ? "bg-primary/10 text-primary" : "bg-destructive/10 text-destructive"
                  }`}
                >
                  {m.badge}
                </span>
              </div>
              <p
                className={`shrink-0 text-[13px] font-black ${
                  m.positive ? "text-primary" : "text-destructive"
                }`}
              >
                {m.amount}
              </p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function LandingHero() {
  return (
    <div className="bg-card relative flex min-h-screen flex-col overflow-hidden">
      {/* Background SVGs */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-[100svh] overflow-hidden"
      >
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src="/landing/fondo-mobile.svg" alt="" className="size-full object-cover lg:hidden" />
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src="/landing/fondo-desktop.svg"
          alt=""
          className="hidden size-full object-cover lg:block"
        />
      </div>

      {/* Header */}
      <header className="relative flex h-[88px] shrink-0 items-center px-4 py-2">
        <div className="flex h-[68px] items-center px-[10px]">
          <div className="border-border bg-card flex h-full items-center rounded-[24px] border px-4 py-[11px]">
            <SplitItLogo />
          </div>
        </div>
      </header>

      {/* Main content */}
      <main className="relative flex flex-1 items-center px-5 lg:px-[140px] lg:pb-[88px]">
        <div className="flex w-full flex-col items-start lg:flex-row lg:items-center lg:justify-between lg:gap-12">
          <div className="flex min-h-[calc(100svh-160px)] flex-col justify-center gap-6 lg:min-h-0 lg:gap-3">
            <h1 className="text-foreground max-w-[295px] text-[36px] leading-[1.15] font-extrabold lg:max-w-[700px] lg:text-[60px]">
              Compartí el link y listo.
            </h1>
            <p className="max-w-[350px] text-[14px] text-black lg:max-w-[560px] lg:text-[18px]">
              Tus amigos cargan gastos y ven sus saldos sin crear cuenta ni bajar nada.
            </p>
            <PrimaryLink href="/register">
              Crear un evento
              <ArrowRight className="size-6" aria-hidden="true" />
            </PrimaryLink>
          </div>

          <BalancePreviewCard />
        </div>
      </main>
    </div>
  );
}

export default function Home() {
  const router = useRouter();
  const session = useSession();

  useEffect(() => {
    if (session.status === "authenticated") {
      router.replace("/eventos");
    }
  }, [session.status, router]);

  if (session.status === "loading" || session.status === "authenticated") {
    return (
      <div className="flex h-screen items-center justify-center">
        <Spinner />
      </div>
    );
  }

  return <LandingHero />;
}
