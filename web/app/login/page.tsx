"use client";
import Image from "next/image";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import { Field, FieldLabel, FieldError, FieldGroup } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/ui/spinner";
import { zodResolver } from "@hookform/resolvers/zod";
import { login, LoginError, LoginInput, loginSchema } from "@/lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const [generalError, setGeneralError] = useState<string | null>(null);
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
      router.push("/dashboard");
    } catch (err) {
      if (err instanceof LoginError) {
        setGeneralError(err.message);
      }
    }
  };

  return (
    <div className="relative grid h-screen md:grid-cols-2">
      <div className="relative hidden h-screen overflow-hidden md:block">
        <Image src="/register-image.png" alt="" fill className="object-cover" />
      </div>

      <div className="flex items-center justify-center overflow-y-auto p-6">
        <div className="w-full max-w-sm">
          <h1 className="text-2xl font-bold">Bienvenido a SplitIt</h1>
          <p className="text-muted-foreground mb-6 text-sm">Ingresá tus datos para continuar</p>

          <form onSubmit={handleSubmit(onSubmit)} noValidate>
            <FieldGroup>
              {generalError && (
                <Alert variant="destructive">
                  <AlertDescription>{generalError}</AlertDescription>
                </Alert>
              )}

              <Field data-invalid={!!errors.email}>
                <FieldLabel htmlFor="email">Email</FieldLabel>
                <Input
                  id="email"
                  placeholder="tu@email.com"
                  autoComplete="email"
                  type="email"
                  aria-invalid={!!errors.email}
                  {...register("email")}
                />
                <FieldError errors={[errors.email]} />
              </Field>

              <Field data-invalid={!!errors.password}>
                <FieldLabel htmlFor="password">Contraseña</FieldLabel>
                <Input
                  id="password"
                  placeholder="Tu contraseña"
                  type="password"
                  autoComplete="current-password"
                  aria-invalid={!!errors.password}
                  {...register("password")}
                />
                <FieldError errors={[errors.password]} />
              </Field>

              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? <Spinner /> : "Ingresar"}
              </Button>
            </FieldGroup>
          </form>
        </div>
      </div>
    </div>
  );
}
