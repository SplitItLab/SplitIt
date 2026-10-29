import Image from "next/image";
import Link from "next/link";

import { SplitItLogo } from "@/components/splitit-logo";
import { Card } from "@/components/ui/card";

type AuthLayoutProps = {
  heroTitle: string;
  heroDescription: string;
  title: string;
  description: string;
  footer: { text: string; linkLabel: string; href: string };
  children: React.ReactNode;
};

export function AuthLayout({
  heroTitle,
  heroDescription,
  title,
  description,
  footer,
  children,
}: AuthLayoutProps) {
  return (
    <div className="grid min-h-svh lg:grid-cols-2">
      <aside className="relative hidden overflow-hidden lg:block">
        <Image src="/register-image.png" alt="" fill unoptimized className="object-cover" />
        <div
          aria-hidden="true"
          className="absolute inset-0 bg-linear-to-t from-black/85 via-black/25 to-transparent"
        />
        <div className="absolute inset-x-0 bottom-0 p-10 xl:p-12">
          <p className="max-w-[460px] text-[32px] leading-[1.15] font-extrabold text-white xl:text-[40px]">
            {heroTitle}
          </p>
          <p className="mt-3 max-w-[400px] text-sm font-medium text-white/80">{heroDescription}</p>
        </div>
      </aside>

      <main className="flex items-center justify-center bg-(--Stale-Gray) px-4 py-10 sm:px-6">
        <div className="w-full max-w-sm">
          <Link
            href="/"
            className="focus-visible:ring-ring/50 inline-flex rounded-[8px] outline-none focus-visible:ring-3"
          >
            <SplitItLogo />
          </Link>

          <h1 className="text-text-primary mt-8 text-[32px] leading-[1.15] font-extrabold">
            {title}
          </h1>
          <p className="text-text-secondary mt-2 text-sm font-medium">{description}</p>

          <Card className="mt-6 rounded-[24px] p-6 shadow-[0_18px_50px_rgba(7,27,58,0.08)] sm:p-7">
            {children}

            <p className="text-text-secondary text-center text-sm">
              {footer.text}{" "}
              <Link className="text-primary font-bold hover:underline" href={footer.href}>
                {footer.linkLabel}
              </Link>
            </p>
          </Card>
        </div>
      </main>
    </div>
  );
}
