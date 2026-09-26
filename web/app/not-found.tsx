"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";

import { PrimaryLink } from "@/components/primary-link";
import { SplitItLogo } from "@/components/splitit-logo";
import { useSession } from "@/lib/use-session";

function NotFoundBackground() {
  return (
    <div aria-hidden="true" className="pointer-events-none absolute inset-0 overflow-hidden">
      <svg
        className="absolute inset-0 size-full"
        viewBox="0 0 1440 900"
        fill="none"
        preserveAspectRatio="xMidYMid slice"
        xmlns="http://www.w3.org/2000/svg"
      >
        <rect width="1440" height="900" fill="#fcfcfe" />

        {/* Top-right large blob */}
        <path
          d="M1200 -80C1320 -20 1460 80 1480 200C1500 320 1400 420 1280 460C1160 500 1020 480 920 420C820 360 760 260 780 160C800 60 900 -20 1020 -60C1080 -80 1140 -100 1200 -80Z"
          fill="url(#notfound-grad1)"
          opacity="0.6"
        />

        {/* Center-left floating shape */}
        <path
          d="M120 300C180 220 300 200 400 240C500 280 560 380 540 480C520 580 440 660 340 680C240 700 140 660 80 580C20 500 60 380 120 300Z"
          fill="url(#notfound-grad2)"
          opacity="0.35"
        />

        {/* Bottom sweeping wave — different from landing */}
        <path
          d="M-40 700C80 620 240 580 440 600C640 620 800 700 1000 680C1200 660 1360 580 1480 620L1480 920L-40 920Z"
          fill="url(#notfound-grad3)"
          opacity="0.45"
        />

        {/* Small accent circle top-left */}
        <circle cx="200" cy="120" r="100" fill="url(#notfound-grad4)" opacity="0.2" />

        {/* Small accent ellipse bottom-right */}
        <ellipse cx="1300" cy="750" rx="160" ry="120" fill="url(#notfound-grad5)" opacity="0.25" />

        <defs>
          <linearGradient
            id="notfound-grad1"
            x1="760"
            y1="-100"
            x2="1480"
            y2="460"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#8855FB" />
            <stop offset="0.5" stopColor="#DFCAFF" />
            <stop offset="1" stopColor="#90DEC6" />
          </linearGradient>

          <linearGradient
            id="notfound-grad2"
            x1="80"
            y1="220"
            x2="540"
            y2="680"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#7ED4EF" />
            <stop offset="0.5" stopColor="#28C49E" />
            <stop offset="1" stopColor="#90DEC6" />
          </linearGradient>

          <linearGradient
            id="notfound-grad3"
            x1="-40"
            y1="600"
            x2="1480"
            y2="800"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#28C49E" />
            <stop offset="0.4" stopColor="#90DEC6" />
            <stop offset="0.8" stopColor="#DFCAFF" />
            <stop offset="1" stopColor="#8855FB" />
          </linearGradient>

          <linearGradient
            id="notfound-grad4"
            x1="100"
            y1="20"
            x2="300"
            y2="220"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#DFCAFF" />
            <stop offset="1" stopColor="#8855FB" />
          </linearGradient>

          <linearGradient
            id="notfound-grad5"
            x1="1140"
            y1="630"
            x2="1460"
            y2="870"
            gradientUnits="userSpaceOnUse"
          >
            <stop stopColor="#28C49E" />
            <stop offset="1" stopColor="#7ED4EF" />
          </linearGradient>
        </defs>
      </svg>
    </div>
  );
}

export default function NotFound() {
  const session = useSession();
  const href = session.status === "authenticated" ? "/eventos" : "/login";

  return (
    <div className="bg-card relative flex min-h-screen flex-col overflow-hidden">
      <NotFoundBackground />

      {/* Header */}
      <header className="relative flex h-[88px] shrink-0 items-center px-4 py-2">
        <div className="flex h-[68px] items-center px-[10px]">
          <Link
            href="/"
            className="border-border bg-card flex h-full items-center rounded-[24px] border px-4 py-[11px]"
          >
            <SplitItLogo />
          </Link>
        </div>
      </header>

      {/* Content */}
      <main className="relative flex flex-1 flex-col items-center justify-center px-5 pb-[88px]">
        <div className="text-center">
          {/* Big 404 with gradient */}
          <p
            className="text-[120px] leading-none font-extrabold tracking-tight sm:text-[180px]"
            style={{
              background: "linear-gradient(135deg, #28C49E 0%, #8855FB 100%)",
              WebkitBackgroundClip: "text",
              WebkitTextFillColor: "transparent",
            }}
          >
            404
          </p>

          <h1 className="text-foreground mt-2 text-[28px] leading-tight font-extrabold sm:text-[36px]">
            Esta página se fue de viaje
          </h1>

          <p className="text-muted-foreground mx-auto mt-3 max-w-[400px] text-[14px] sm:text-[16px]">
            No pudimos encontrar lo que buscás. Puede que la dirección esté mal escrita o que la
            página ya no exista.
          </p>

          <PrimaryLink href={href} size="lg" className="mt-8">
            <ArrowLeft className="size-5" aria-hidden="true" />
            Volver al inicio
          </PrimaryLink>
        </div>
      </main>
    </div>
  );
}
