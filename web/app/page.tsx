import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export default function Home() {
  return (
    <div className="flex h-screen items-center justify-center p-6">
      <div className="w-full max-w-sm">
        <h1 className="text-2xl font-bold">SplitIt</h1>
        <p className="text-muted-foreground mb-6 text-sm">Dividí gastos con tu grupo.</p>
        <div className="flex flex-col gap-3">
          <Link href="/login" className={cn(buttonVariants())}>
            Ingresar
          </Link>
          <Link href="/register" className={cn(buttonVariants({ variant: "outline" }))}>
            Crear cuenta
          </Link>
        </div>
      </div>
    </div>
  );
}
