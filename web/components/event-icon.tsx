import { createElement } from "react";

import {
  Beer,
  Briefcase,
  CalendarDays,
  Car,
  Flame,
  Gift,
  Heart,
  House,
  Music,
  PartyPopper,
  Plane,
  Ship,
  ShoppingBag,
  Tent,
  UtensilsCrossed,
  type LucideIcon,
} from "lucide-react";

import { cn } from "@/lib/utils";

/** Iconos que ofrece el selector al crear un evento. */
export const EVENT_ICON_OPTIONS: { key: string; label: string; Icon: LucideIcon }[] = [
  { key: "plane", label: "Viaje", Icon: Plane },
  { key: "house", label: "Hogar", Icon: House },
  { key: "party", label: "Fiesta", Icon: PartyPopper },
  { key: "food", label: "Comida", Icon: UtensilsCrossed },
  { key: "car", label: "Transporte", Icon: Car },
  { key: "tent", label: "Camping", Icon: Tent },
  { key: "gift", label: "Regalo", Icon: Gift },
  { key: "shopping", label: "Compras", Icon: ShoppingBag },
  { key: "music", label: "Concierto", Icon: Music },
  { key: "heart", label: "Pareja", Icon: Heart },
  { key: "drinks", label: "Salida", Icon: Beer },
  { key: "work", label: "Trabajo", Icon: Briefcase },
];

/** Claves adicionales que puede devolver la API y no están en el selector. */
const EXTRA_ICONS: Record<string, LucideIcon> = {
  grill: Flame,
  ship: Ship,
};

const ICONS_BY_KEY: Record<string, LucideIcon> = {
  ...Object.fromEntries(EVENT_ICON_OPTIONS.map(({ key, Icon }) => [key, Icon])),
  ...EXTRA_ICONS,
};

export function eventIconFor(iconKey?: string | null): LucideIcon {
  if (!iconKey) return CalendarDays;
  return ICONS_BY_KEY[iconKey] ?? CalendarDays;
}

export function EventIcon({ iconKey, className }: { iconKey?: string | null; className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        "bg-primary/10 text-primary flex size-14 shrink-0 items-center justify-center rounded-[16px]",
        className
      )}
    >
      {createElement(eventIconFor(iconKey), { className: "size-6" })}
    </span>
  );
}
