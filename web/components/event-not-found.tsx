import Link from "next/link";
import { SearchX } from "lucide-react";

import { buttonVariants } from "@/components/ui/button";

export function EventNotFound() {
  return (
    <section className="border-border rounded-[24px] border p-6 sm:p-8">
      <div className="mx-auto max-w-sm text-center">
        <span
          aria-hidden="true"
          className="bg-muted text-text-secondary mx-auto mb-3 flex size-12 items-center justify-center rounded-[16px]"
        >
          <SearchX className="size-5" />
        </span>
        <h1 className="text-text-primary text-2xl font-extrabold">No encontramos este evento</h1>
        <p className="text-text-secondary mt-2 text-sm font-medium">
          Puede que lo hayan eliminado, que el link esté incompleto, o que no estés invitado. Revisá
          el link con quien te lo compartió.
        </p>
        <Link
          href="/eventos"
          className={buttonVariants({
            className: "mt-5 h-10 rounded-[8px] px-5 text-base font-medium",
          })}
        >
          Ir a mis eventos
        </Link>
      </div>
    </section>
  );
}
