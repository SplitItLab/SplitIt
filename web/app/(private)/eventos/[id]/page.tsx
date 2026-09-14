import Link from "next/link";
import { ArrowLeft } from "lucide-react";

export default async function EventDetailPage({ params }: PageProps<"/eventos/[id]">) {
  const { id } = await params;

  return (
    <div className="space-y-6 px-6 pt-4 pb-16 sm:px-10 sm:pt-6 lg:space-y-8">
      <Link
        href="/eventos"
        className="text-text-secondary hover:text-text-primary inline-flex items-center gap-2 text-sm font-medium"
      >
        <ArrowLeft className="size-4" />
        Volver a eventos
      </Link>

      <header>
        <p className="text-primary text-sm font-medium">Evento #{id}</p>
        <h1 className="text-text-primary mt-1 text-[40px] leading-[1.15] font-extrabold sm:text-[60px]">
          Detalle del evento
        </h1>
        <p className="text-text-secondary mt-2 max-w-xl text-sm font-medium">
          Administrá los gastos del grupo y revisá los saldos de este evento.
        </p>
      </header>

      <div className="grid gap-4 lg:grid-cols-2">
        <section className="border-border rounded-[24px] border p-6">
          <h2 className="text-text-primary text-2xl font-extrabold">Gastos</h2>
          <p className="text-text-secondary mt-2 text-sm font-medium">
            Los gastos del evento se mostrarán acá.
          </p>
        </section>
        <section className="border-border rounded-[24px] border p-6">
          <h2 className="text-text-primary text-2xl font-extrabold">Saldos</h2>
          <p className="text-text-secondary mt-2 text-sm font-medium">
            Los saldos entre integrantes se mostrarán acá.
          </p>
        </section>
      </div>
    </div>
  );
}
