import { PrivateRoute } from "@/components/private-route";
import { AppHeader } from "@/components/app-header";

export default function PrivateLayout({ children }: { children: React.ReactNode }) {
  return (
    <PrivateRoute>
      <div className="flex min-h-screen flex-col">
        <AppHeader />
        <main className="flex-1">{children}</main>
      </div>
    </PrivateRoute>
  );
}
