"use client";

import { useId, useRef, useState } from "react";
import Link from "next/link";
import { ArrowLeft, UserRound } from "lucide-react";

import { currencyLabel, type EventDetail, type EventMember } from "@/lib/events";
import { getInitials } from "@/lib/profile";
import { EventIcon } from "@/components/event-icon";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";

const TABS = [
  { id: "gastos", label: "Gastos" },
  { id: "saldos", label: "Saldos" },
  { id: "integrantes", label: "Integrantes" },
] as const;

type TabId = (typeof TABS)[number]["id"];

export function EventDetailContent({ event }: { event: EventDetail }) {
  const [active, setActive] = useState<TabId>("gastos");
  const baseId = useId();
  const tabRefs = useRef<Record<string, HTMLButtonElement | null>>({});

  const tabId = (id: TabId) => `${baseId}-tab-${id}`;
  const panelId = (id: TabId) => `${baseId}-panel-${id}`;

  const onTabKeyDown = (event_: React.KeyboardEvent) => {
    const order = TABS.map((tab) => tab.id);
    const current = order.indexOf(active);
    let next: number | null = null;

    if (event_.key === "ArrowRight") next = (current + 1) % order.length;
    if (event_.key === "ArrowLeft") next = (current - 1 + order.length) % order.length;
    if (event_.key === "Home") next = 0;
    if (event_.key === "End") next = order.length - 1;
    if (next === null) return;

    event_.preventDefault();
    const id = order[next];
    setActive(id);
    tabRefs.current[id]?.focus();
  };

  return (
    <>
      <Link
        href="/eventos"
        aria-label="Volver a eventos"
        className="border-border text-text-secondary hover:text-text-primary hover:bg-muted focus-visible:ring-ring/50 inline-flex size-12 items-center justify-center rounded-full border outline-none focus-visible:ring-3"
      >
        <ArrowLeft className="size-5" />
      </Link>

      <header className="flex items-start gap-4">
        <EventIcon iconKey={event.iconKey} />
        <div className="min-w-0 flex-1">
          <h1 className="text-text-primary text-[32px] leading-[1.15] font-extrabold break-words sm:text-[40px]">
            {event.name}
          </h1>
          {event.description && (
            <p className="text-text-secondary mt-1 max-w-xl text-sm font-medium">
              {event.description}
            </p>
          )}
          <Badge variant="info" className="mt-3 h-auto rounded-full px-3 py-1 font-black">
            Moneda {currencyLabel(event.baseCurrency)}
          </Badge>
        </div>
      </header>

      <div className="border-border border-b">
        <div role="tablist" aria-label="Secciones del evento" className="flex gap-6">
          {TABS.map((tab) => {
            const selected = tab.id === active;
            return (
              <button
                key={tab.id}
                ref={(node) => {
                  tabRefs.current[tab.id] = node;
                }}
                type="button"
                role="tab"
                id={tabId(tab.id)}
                aria-selected={selected}
                aria-controls={panelId(tab.id)}
                tabIndex={selected ? 0 : -1}
                onClick={() => setActive(tab.id)}
                onKeyDown={onTabKeyDown}
                className={`focus-visible:ring-ring/50 -mb-px border-b-2 px-1 pb-3 text-base font-bold outline-none focus-visible:ring-3 ${
                  selected
                    ? "border-primary text-primary"
                    : "text-text-secondary hover:text-text-primary border-transparent"
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      {active === "gastos" && (
        <section role="tabpanel" id={panelId("gastos")} aria-labelledby={tabId("gastos")}>
          <h2 className="text-text-primary text-2xl font-extrabold">Gastos</h2>
          <div className="border-border mt-4 rounded-[24px] border p-6 sm:p-8">
            <div className="mx-auto max-w-sm text-center">
              <h3 className="text-text-primary text-2xl font-extrabold">Todavía no hay gastos</h3>
              <p className="text-text-secondary mt-2 text-sm font-medium">
                Cuando cargues gastos, van a aparecer acá para revisar quién pagó y cuánto
                corresponde.
              </p>
            </div>
          </div>
        </section>
      )}

      {active === "saldos" && (
        <section role="tabpanel" id={panelId("saldos")} aria-labelledby={tabId("saldos")}>
          <h2 className="text-text-primary text-2xl font-extrabold">Saldos</h2>
          <div className="border-border mt-4 rounded-[24px] border p-6 sm:p-8">
            <div className="mx-auto max-w-sm text-center">
              <h3 className="text-text-primary text-2xl font-extrabold">Todavía no hay saldos</h3>
              <p className="text-text-secondary mt-2 text-sm font-medium">
                Cuando el evento tenga gastos cargados, acá vas a ver cuánto le corresponde a cada
                integrante.
              </p>
            </div>
          </div>
        </section>
      )}

      {active === "integrantes" && (
        <section role="tabpanel" id={panelId("integrantes")} aria-labelledby={tabId("integrantes")}>
          <h2 className="text-text-primary text-2xl font-extrabold">Integrantes</h2>
          <ul className="mt-4 grid gap-3 md:grid-cols-2">
            {event.members.map((member) => (
              <MemberCard key={member.id} member={member} />
            ))}
          </ul>
        </section>
      )}
    </>
  );
}

function MemberCard({ member }: { member: EventMember }) {
  return (
    <li className="border-border flex items-center gap-3 rounded-[24px] border p-4">
      <Avatar className="size-10">
        <AvatarFallback>{getInitials(member.name)}</AvatarFallback>
      </Avatar>
      <div className="min-w-0 flex-1">
        <p className="text-text-primary truncate text-base font-extrabold">{member.name}</p>
        {member.isGuest ? (
          <p className="text-text-secondary mt-0.5 text-sm font-medium">Invitado</p>
        ) : (
          member.email && (
            <p className="text-text-secondary mt-0.5 truncate text-sm font-medium">
              {member.email}
            </p>
          )
        )}
      </div>
      <UserRound aria-hidden="true" className="text-muted-foreground size-5 shrink-0" />
    </li>
  );
}
