"use client";
import { Suspense, useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter, useSearchParams } from "next/navigation";
import { Field, FieldLabel, FieldError, FieldGroup } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { LoadingIndicator } from "@/components/loading-indicator";
import { AuthLayout } from "@/components/auth-layout";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  login,
  LoginError,
  LoginInput,
  loginSchema,
  postLoginPath,
  withRedirect,
} from "@/lib/auth";
import { Eye, EyeOff } from "lucide-react";

export default function LoginPage() {
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  );
}

function LoginForm() {
  const router = useRouter();
  const redirect = useSearchParams().get("redirect");
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginInput) => {
    setGeneralError(null);
    try {
      await login(data);
      router.push(postLoginPath(redirect));
    } catch (err) {
      if (err instanceof LoginError) {
        setGeneralError(err.message);
      }
    }
  };

  return (
    <AuthLayout
      heroTitle="Tus cuentas claras, sin vueltas."
      heroDescription="Entrá y mirá al toque quién pagó qué y cuánto le debe cada uno."
      title="Iniciá sesión"
      description="Ingresá tus datos para continuar"
      footer={{
        text: "¿No tenés cuenta?",
        linkLabel: "Registrate gratis",
        href: withRedirect("/register", redirect),
      }}
    >
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <FieldGroup>
          {generalError && (
            <Alert variant="destructive">
              <AlertDescription>{generalError}</AlertDescription>
            </Alert>
          )}

          <Field className="gap-1.5" data-invalid={!!errors.email}>
            <FieldLabel htmlFor="email" className="font-bold">
              Email
            </FieldLabel>
            <Input
              id="email"
              placeholder="tu@email.com"
              autoComplete="email"
              type="email"
              aria-invalid={!!errors.email}
              className="h-11"
              {...register("email")}
            />
            <FieldError errors={[errors.email]} />
          </Field>

          <Field className="gap-1.5" data-invalid={!!errors.password}>
            <FieldLabel htmlFor="password" className="font-bold">
              Contraseña
            </FieldLabel>
            <div className="relative">
              <Input
                id="password"
                placeholder="Tu contraseña"
                type={showPassword ? "text" : "password"}
                autoComplete="current-password"
                aria-invalid={!!errors.password}
                className="h-11 pr-10"
                {...register("password")}
              />
              <button
                type="button"
                className="text-muted-foreground hover:text-foreground absolute top-1/2 right-3 -translate-y-1/2"
                onClick={() => setShowPassword((prev) => !prev)}
                aria-label={showPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
              >
                {showPassword ? (
                  <EyeOff className="h-4 w-4" aria-hidden="true" />
                ) : (
                  <Eye className="h-4 w-4" aria-hidden="true" />
                )}
              </button>
            </div>
            <FieldError errors={[errors.password]} />
          </Field>

          <Button type="submit" className="h-11 w-full" disabled={isSubmitting}>
            {isSubmitting ? <LoadingIndicator message="Validando tus datos…" /> : "Ingresar"}
          </Button>
        </FieldGroup>
      </form>
    </AuthLayout>
  );
}
