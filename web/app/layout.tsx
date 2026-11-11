import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { YbugWidget } from "@/components/ybug-widget";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const DESCRIPTION =
  "Armá un evento, cargá los gastos y SplitIt calcula quién le debe a quién. Sin planillas ni discusiones.";

export const metadata: Metadata = {
  // Las imágenes de la vista previa necesitan URL absoluta; sin la variable, Next usa la de Vercel.
  ...(process.env.NEXT_PUBLIC_SITE_URL && {
    metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL),
  }),
  title: "SplitIt",
  description: DESCRIPTION,
  openGraph: { title: "SplitIt", description: DESCRIPTION, siteName: "SplitIt", type: "website" },
  manifest: "/site.webmanifest",
  icons: {
    icon: [
      { url: "/favicon.ico" },
      { url: "/favicon-16x16.png", sizes: "16x16", type: "image/png" },
      { url: "/favicon-32x32.png", sizes: "32x32", type: "image/png" },
    ],
    apple: [{ url: "/apple-touch-icon.png", sizes: "180x180" }],
  },
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="es" className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}>
      <body className="min-h-full">
        {children}
        <YbugWidget />
      </body>
    </html>
  );
}
