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
  registerUser,
  RegisterError,
  RegisterInput,
  registerSchema,
  withRedirect,
} from "@/lib/auth";
import { showAppToast } from "@/lib/toast";

export default function RegisterPage() {
  return (
    <Suspense>
      <RegisterForm />
    </Suspense>
  );
}

function RegisterForm() {
  const router = useRouter();
  const loginPath = withRedirect("/login", useSearchParams().get("redirect"));
  const [generalError, setGeneralError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterInput>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data: RegisterInput) => {
    setGeneralError(null);
    try {
      await registerUser(data);
      showAppToast("success", "¡Cuenta creada con éxito!");
      router.push(loginPath);
    } catch (err) {
      if (err instanceof RegisterError) {
        if (err.type === "email-taken") {
          setError("email", { message: err.message });
        } else {
          setGeneralError(err.message);
        }
        showAppToast("error", err.message);
      }
    }
  };

  return (
    <AuthLayout
      heroTitle="Empezá a dividir gastos en menos de un minuto."
      heroDescription="Creá tu cuenta, sumá a tu grupo y olvidate de las cuentas pendientes."
      title="Crea una cuenta"
      description="Ingresá mail y contraseña para crear tu cuenta"
      footer={{ text: "¿Ya tenés cuenta?", linkLabel: "Iniciá sesión", href: loginPath }}
    >
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <FieldGroup>
          {generalError && (
            <Alert variant="destructive">
              <AlertDescription>{generalError}</AlertDescription>
            </Alert>
          )}

          <Field className="gap-1.5" data-invalid={!!errors.name}>
            <FieldLabel htmlFor="name" className="font-bold">
              Nombre completo
            </FieldLabel>
            <Input
              id="name"
              placeholder="Tu nombre"
              autoComplete="name"
              aria-invalid={!!errors.name}
              className="h-11"
              {...register("name")}
            />
            <FieldError errors={[errors.name]} />
          </Field>

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
            <Input
              id="password"
              placeholder="Creá una contraseña"
              type="password"
              autoComplete="new-password"
              aria-invalid={!!errors.password}
              className="h-11"
              {...register("password")}
            />
            <FieldError errors={[errors.password]} />
          </Field>

          <Button type="submit" className="h-11 w-full" disabled={isSubmitting}>
            {isSubmitting ? <LoadingIndicator message="Creando tu cuenta…" /> : "Crear cuenta"}
          </Button>
        </FieldGroup>
      </form>
    </AuthLayout>
  );
}
